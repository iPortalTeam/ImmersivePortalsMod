package qouteall.imm_ptl.core.chunk_loading;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.mixin.common.chunk_sync.IEServerCommonPacketListenerImpl;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ImmPtlChunkTracking {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final int updateInterval = 13;
    public static final int defaultDelayUnloadGenerations = 4;
    
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ImmPtlChunkTracking::tick);
        IPGlobal.SERVER_CLEANUP_EVENT.register(ImmPtlChunkTracking::cleanup);
        
        DimensionAPI.SERVER_PRE_REMOVE_DIMENSION_EVENT.register(
            ImmPtlChunkTracking::onDimensionRemove
        );
    }
    
    public static void onChunkProvidedDeferred(LevelChunk chunk) {
        // do nothing for now
    }
    
    // if the player object is recreated, pass in the old player object
    public static void removePlayerFromChunkTrackersAndEntityTrackers(ServerPlayer oldPlayer) {
        LOGGER.info("Removing from chunk tracking {}", oldPlayer);
        
        for (ServerLevel world : oldPlayer.server.getAllLevels()) {
            ServerChunkCache chunkManager = world.getChunkSource();
            IEChunkMap storage =
                (IEChunkMap) chunkManager.chunkMap;
            storage.ip_onPlayerUnload(oldPlayer);
        }
        
        forceRemovePlayer(oldPlayer);
    }
    
    public static void onDimensionRemove(ServerLevel world) {
        ServerChunkCache chunkManager = (ServerChunkCache) world.getChunkSource();
        IEChunkMap storage =
            (IEChunkMap) chunkManager.chunkMap;
        storage.ip_onDimensionRemove();
        
        forceRemoveDimension(world.dimension());
    }
    
    public static class PlayerWatchRecord {
        public final ServerPlayer player;
        public final ResourceKey<Level> dimension;
        public final long chunkPos;
        public int lastWatchGeneration;
        public int distanceToSource;
        public boolean isLoadedToPlayer;
        public boolean isValid = true;
        public boolean isBoundary = false;
        // the light data is only sent on visibility boundary
        // as the client can calculate light from block data
        
        public PlayerWatchRecord(
            ServerPlayer player, ResourceKey<Level> dimension,
            long chunkPos, int lastWatchGeneration,
            int distanceToSource, boolean isLoadedToPlayer,
            boolean isBoundary
        ) {
            this.player = player;
            this.dimension = dimension;
            this.chunkPos = chunkPos;
            this.lastWatchGeneration = lastWatchGeneration;
            this.distanceToSource = distanceToSource;
            this.isLoadedToPlayer = isLoadedToPlayer;
            this.isBoundary = isBoundary;
        }
        
        @Override
        public String toString() {
            return String.format(
                "%s (%d,%d) distance:%d valid:%s loaded:%s",
                dimension.location(),
                ChunkPos.getX(chunkPos),
                ChunkPos.getZ(chunkPos),
                distanceToSource,
                isValid,
                isLoadedToPlayer
            );
        }
    }
    
    // Every chunk has a list of watching records
    private static final Map<
        ResourceKey<Level>,
        Long2ObjectOpenHashMap<
            Object2ObjectOpenHashMap<ServerPlayer, PlayerWatchRecord>>> chunkWatchRecords =
        new Object2ObjectOpenHashMap<>();
    
    private static final ArrayList<ChunkLoader> additionalChunkLoaders = new ArrayList<>();
    
    private static final Object2ObjectOpenHashMap<ServerPlayer, PlayerChunkLoading> playerInfoMap =
        new Object2ObjectOpenHashMap<>();
    
    private static int generationCounter = 0;
    
    private static Long2ObjectOpenHashMap<Object2ObjectOpenHashMap<ServerPlayer, PlayerWatchRecord>>
    getDimChunkWatchRecords(ResourceKey<Level> dimension) {
        return chunkWatchRecords.computeIfAbsent(dimension, k -> new Long2ObjectOpenHashMap<>());
    }
    
    public static PlayerChunkLoading getPlayerInfo(ServerPlayer player) {
        return playerInfoMap.computeIfAbsent(
            player,
            (ServerPlayer p) -> new PlayerChunkLoading(
                ((IEServerCommonPacketListenerImpl) p.connection)
                    .ip_getConnection().isMemoryConnection()
            )
        );
    }
    
    public static void immediatelyUpdateForPlayer(ServerPlayer player) {
        ImmPtlChunkTracking.updateForPlayer(player);
        
        getPlayerInfo(player).doChunkSending(player);
        
        // must call after chunk tracking update and chunk packet sending
        // (not sending chunk packet disallows entity tracking)
        // we need to send add entity packet early,
        // otherwise player will fall when standing on cross-portal-collision when logging in
        EntitySync.update(player.server);
    }
    
    public static void updateForPlayer(ServerPlayer player) {
        PlayerChunkLoading playerInfo = getPlayerInfo(player);
        playerInfo.visibleDimensions.clear();
        int lastLoadedChunks = playerInfo.loadedChunks;
        playerInfo.loadedChunks = 0;
        
        ObjectOpenHashSet<ChunkLoader> chunkLoaders = new ObjectOpenHashSet<>();
        
        ChunkVisibility.foreachBaseChunkLoaders(
            player,
            chunkLoaders::add
        );
        
        chunkLoaders.addAll(playerInfo.additionalChunkLoaders);
        
        MinecraftServer server = MiscHelper.getServer();
        
        for (ChunkLoader chunkLoader : chunkLoaders) {
            ResourceKey<Level> dimension = chunkLoader.dimension();
            var chunkRecordMap = getDimChunkWatchRecords(dimension);
            
            ServerLevel world = server.getLevel(dimension);
            if (world == null) {
                LOGGER.warn("Dimension not loaded {} in chunk loader {}", dimension, chunkLoader);
                return;
            }
            
            playerInfo.visibleDimensions.add(dimension);
            
            ImmPtlChunkTickets ticketInfo = ImmPtlChunkTickets.get(world);
            
            chunkLoader.foreachChunkPos((dim, x, z, distanceToSource) -> {
                long chunkPos = ChunkPos.asLong(x, z);
                var records =
                    chunkRecordMap.computeIfAbsent(chunkPos, k -> new Object2ObjectOpenHashMap<>());
                
                ticketInfo.markForLoading(chunkPos, distanceToSource, generationCounter);
                
                records.compute(player, (k, record) -> {
                    boolean isBoundary = distanceToSource == chunkLoader.radius();
                    if (record == null) {
                        PlayerWatchRecord newRecord = new PlayerWatchRecord(
                            player, dimension, chunkPos, generationCounter, distanceToSource,
                            false, isBoundary
                        );
                        playerInfo.markPendingLoading(newRecord);
                        playerInfo.loadedChunks++;
                        return newRecord;
                    }
                    else {
                        int oldDistance = record.distanceToSource;
                        if (record.lastWatchGeneration == generationCounter) {
                            // being updated again in the same turn
                            if (distanceToSource < oldDistance) {
                                record.distanceToSource = distanceToSource;
                                playerInfo.markPendingLoading(record);
                            }
                            
                            record.isBoundary = (record.isBoundary && isBoundary);
                        }
                        else {
                            // being updated at the first time in this turn
                            playerInfo.loadedChunks++;
                            if (distanceToSource < oldDistance) {
                                playerInfo.markPendingLoading(record);
                            }
                            
                            record.distanceToSource = distanceToSource;
                            record.lastWatchGeneration = generationCounter;
                            record.isBoundary = isBoundary;
                        }
                    }
                    
                    return record;
                });
            });
        }
    }
    
    private static void purge(
        Object2ObjectOpenHashMap<ResourceKey<Level>, LongOpenHashSet> additionalLoadedChunks
    ) {
        // purge chunk watch records
        chunkWatchRecords.forEach((dimension, chunkRecords) -> {
            chunkRecords.long2ObjectEntrySet().removeIf(entry -> {
                long chunkPosLong = entry.getLongKey();
                
                var dimChunkWatchRecords = entry.getValue();
                
                dimChunkWatchRecords.entrySet().removeIf(e -> {
                    ServerPlayer player = e.getKey();
                    
                    if (player.isRemoved()) {
                        return true;
                    }
                    
                    PlayerWatchRecord record = e.getValue();
                    int delayUnloadGenerations = getDelayUnloadGenerationForPlayer(player);
                    boolean shouldRemove = generationCounter - record.lastWatchGeneration > delayUnloadGenerations;
                    
                    if (shouldRemove) {
                        if (record.isLoadedToPlayer) {
                            player.connection.send(
                                PacketRedirection.createRedirectedMessage(
                                    player.getServer(),
                                    record.dimension,
                                    new ClientboundForgetLevelChunkPacket(
                                        new ChunkPos(record.chunkPos)
                                    )
                                )
                            );
                        }
                        record.isValid = false;
                    }
                    
                    return shouldRemove;
                });
                
                return dimChunkWatchRecords.isEmpty();
            });
        });
        
        // purge player info map
        playerInfoMap.entrySet().removeIf(e -> e.getKey().isRemoved());
        
        MinecraftServer server = MiscHelper.getServer();
        for (ServerLevel world : server.getAllLevels()) {
            ResourceKey<Level> dimension = world.dimension();
            
            @Nullable LongOpenHashSet additional = additionalLoadedChunks.get(dimension);
            @Nullable var watchRecs =
                chunkWatchRecords.get(dimension);
            
            ImmPtlChunkTickets dimTicketManager = ImmPtlChunkTickets.get(world);
            
            dimTicketManager.purge(
                world,
                chunkPos -> {
                    if (watchRecs != null && watchRecs.containsKey(chunkPos)) {
                        return true;
                    }
                    if (additional != null && additional.contains(chunkPos)) {
                        return true;
                    }
                    return false;
                }
            );
        }
    }
    
    // unload chunks earlier if the player loads many chunks
    private static int getDelayUnloadGenerationForPlayer(ServerPlayer player) {
        PlayerChunkLoading playerInfo = getPlayerInfo(player);
        if (playerInfo == null) {
            return defaultDelayUnloadGenerations;
        }
        
        int loadedChunks = playerInfo.loadedChunks;
        
        if (loadedChunks > 2000) {
            return 1;
        }
        
        if (loadedChunks > 1200) {
            return 2;
        }
        
        return defaultDelayUnloadGenerations;
    }
    
    private static Object2ObjectOpenHashMap<ResourceKey<Level>, LongOpenHashSet> refreshAdditionalChunkLoaders() {
        Object2ObjectOpenHashMap<ResourceKey<Level>, LongOpenHashSet> additionalLoadedChunks =
            new Object2ObjectOpenHashMap<>();
        
        additionalChunkLoaders.removeIf(chunkLoader -> {
            ResourceKey<Level> dimension = chunkLoader.dimension();
            ServerLevel world = MiscHelper.getServer().getLevel(dimension);
            
            if (world == null) {
                LOGGER.error("Missing dimension in chunk loader {}", dimension.location());
                return true;
            }
            
            ImmPtlChunkTickets dimTicketManager = ImmPtlChunkTickets.get(world);
            
            LongOpenHashSet set = additionalLoadedChunks.computeIfAbsent(dimension, k -> new LongOpenHashSet());
            
            chunkLoader.foreachChunkPos(new ChunkLoader.ChunkPosConsumer() {
                @Override
                public void consume(ResourceKey<Level> dimension, int x, int z, int distanceToSource) {
                    long chunkPos = ChunkPos.asLong(x, z);
                    dimTicketManager.markForLoading(chunkPos, distanceToSource, generationCounter);
                    set.add(chunkPos);
                }
            });
            
            return false;
        });
        
        return additionalLoadedChunks;
    }
    
    private static void tick(MinecraftServer server) {
        server.getProfiler().push("portal_chunk_tracking");
        
        boolean updates = false;
        long gameTime = McHelper.getOverWorldOnServer().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerChunkLoading playerInfo = getPlayerInfo(player);
            
            // spread the player updates to different ticks
            if (playerInfo.shouldUpdateImmediately ||
                ((player.getId() % updateInterval) == (gameTime % updateInterval))
            ) {
                playerInfo.shouldUpdateImmediately = false;
                updateForPlayer(player);
                updates = true;
            }
        }
        if (gameTime % updateInterval == 0) {
            var additionalLoadedChunks = refreshAdditionalChunkLoaders();
            purge(additionalLoadedChunks);
            generationCounter++;
            updates = true;
        }
        
        for (ServerLevel world : MiscHelper.getServer().getAllLevels()) {
            ImmPtlChunkTickets dimTicketManager = ImmPtlChunkTickets.get(world);
            
            dimTicketManager.tick(world);
        }
        
        server.getProfiler().pop();
        
        if (updates) {
            EntitySync.update(server);
        }
        
        EntitySync.tick(server);
    }
    
    public static boolean isPlayerWatchingChunk(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        int x, int z,
        Predicate<PlayerWatchRecord> predicate
    ) {
        long chunkPos = ChunkPos.asLong(x, z);
        
        var recordMap = getDimChunkWatchRecords(dimension).get(chunkPos);
        if (recordMap == null) {
            return false;
        }
        
        PlayerWatchRecord record = recordMap.get(player);
        
        if (record == null) {
            return false;
        }
        
        if (!record.isLoadedToPlayer) {
            return false;
        }
        
        return predicate.test(record);
    }
    
    public static boolean isPlayerWatchingChunk(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        int x, int z
    ) {
        return isPlayerWatchingChunk(player, dimension, x, z, r -> true);
    }
    
    public static boolean isPlayerWatchingChunkWithinRadius(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        int x, int z,
        int radiusBlocks
    ) {
        return isPlayerWatchingChunk(
            player, dimension, x, z,
            r -> r.distanceToSource * 16 <= radiusBlocks
        );
    }
    
    private static void cleanup(MinecraftServer server) {
        chunkWatchRecords.clear();
        additionalChunkLoaders.clear();
        playerInfoMap.clear();
    }
    
    /**
     * Note when update should also check {@link qouteall.imm_ptl.core.mixin.common.other_sync.MixinPlayerList}
     */
    public static Stream<ServerPlayer> getPlayersViewingChunk(
        ResourceKey<Level> dimension,
        int x, int z
    ) {
        var records = getWatchRecordForChunk(dimension, x, z);
        if (records == null) {
            return Stream.empty();
        }
        return records.values().stream().filter(e -> e.isLoadedToPlayer).map(e -> e.player);
    }
    
    public static List<ServerPlayer> getPlayersViewingChunk(
        ResourceKey<Level> dimension,
        int x, int z,
        boolean boundaryOnly
    ) {
        var recs =
            ImmPtlChunkTracking.getWatchRecordForChunk(dimension, x, z);
        
        if (recs == null) {
            return Collections.emptyList();
        }
        
        // the boundaryOnly parameter is only true when sending light update packets
        // the client can calculate the light by the block data, but not accurate on loading boundary
        
        ArrayList<ServerPlayer> result = new ArrayList<>();
        for (ImmPtlChunkTracking.PlayerWatchRecord rec : recs.values()) {
            if (rec.isLoadedToPlayer && (!boundaryOnly || rec.isBoundary)) {
                result.add(rec.player);
            }
        }
        
        return result;
    }
    
    // Note all PlayerWatchRecord taken from it are valid
    @Nullable
    public static Object2ObjectOpenHashMap<ServerPlayer, PlayerWatchRecord> getWatchRecordForChunk(
        ResourceKey<Level> dimension, int x, int z
    ) {
        return getDimChunkWatchRecords(dimension).get(ChunkPos.asLong(x, z));
    }
    
    public static void forceRemovePlayer(ServerPlayer oldPlayer) {
        playerInfoMap.remove(oldPlayer);
        
        chunkWatchRecords.forEach((dim, dimMap) -> {
            dimMap.long2ObjectEntrySet().removeIf(e -> {
                long chunkPos = e.getLongKey();
                Object2ObjectOpenHashMap<ServerPlayer, PlayerWatchRecord> records = e.getValue();
                PlayerWatchRecord rec = records.remove(oldPlayer);
                if (rec != null) {
                    PacketRedirection.sendRedirectedMessage(
                        oldPlayer, dim, new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkPos))
                    );
                }
                
                return records.isEmpty();
            });
        });
    }
    
    public static void forceRemoveDimension(ResourceKey<Level> dim) {
        var map =
            chunkWatchRecords.get(dim);
        
        if (map == null) {
            return;
        }
        
        map.forEach((chunkPos, records) -> {
            var unloadPacket = PacketRedirection.createRedirectedMessage(
                MiscHelper.getServer(),
                dim, new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkPos))
            );
            for (PlayerWatchRecord record : records.values()) {
                if (record.isValid && record.isLoadedToPlayer) {
                    record.player.connection.send(unloadPacket);
                }
                record.isValid = false;
            }
        });
        
        chunkWatchRecords.remove(dim);
        
        additionalChunkLoaders.removeIf(chunkLoader -> {
            return chunkLoader.dimension() == dim;
        });
        
        for (PlayerChunkLoading playerInfo : playerInfoMap.values()) {
            playerInfo.additionalChunkLoaders.removeIf(l -> l.dimension() == dim);
        }
    }
    
    public static boolean shouldLoadDimension(ResourceKey<Level> dimension) {
        if (!chunkWatchRecords.containsKey(dimension)) {
            return false;
        }
        var map =
            chunkWatchRecords.get(dimension);
        return !map.isEmpty();
    }
    
    public static void addGlobalAdditionalChunkLoader(
        MinecraftServer server,
        ChunkLoader chunkLoader
    ) {
        additionalChunkLoaders.add(chunkLoader);
        
        ResourceKey<Level> dimension = chunkLoader.dimension();
        ServerLevel world = server.getLevel(dimension);
        
        if (world == null) {
            LOGGER.error("Missing dimension in chunk loader {}", dimension.location());
            return;
        }
        
        ImmPtlChunkTickets dimTicketManager = ImmPtlChunkTickets.get(world);
        
        chunkLoader.foreachChunkPos((dim, x, z, distanceToSource) -> {
            dimTicketManager.markForLoading(ChunkPos.asLong(x, z), distanceToSource, generationCounter);
        });
    }
    
    /**
     * NOTE it removes chunk loader by object reference, not by value equality
     */
    public static void removeGlobalAdditionalChunkLoader(
        MinecraftServer server, ChunkLoader chunkLoader
    ) {
        additionalChunkLoaders.removeIf(c -> c == chunkLoader);
    }
    
    public static int getLoadedChunkNum(ResourceKey<Level> dimension) {
        return getDimChunkWatchRecords(dimension).size();
    }
    
    public static void addPerPlayerAdditionalChunkLoader(
        ServerPlayer player, ChunkLoader chunkLoader
    ) {
        PlayerChunkLoading playerInfo = getPlayerInfo(player);
        playerInfo.additionalChunkLoaders.add(chunkLoader);
        playerInfo.shouldUpdateImmediately = true;
    }
    
    /**
     * NOTE it removes chunk loader by object reference, not by value equality
     */
    public static void removePerPlayerAdditionalChunkLoader(
        ServerPlayer player, ChunkLoader chunkLoader
    ) {
        ArrayList<ChunkLoader> chunkLoaderList = getPlayerInfo(player).additionalChunkLoaders;
        chunkLoaderList.removeIf(c -> c == chunkLoader);
    }
    
    public static Set<ResourceKey<Level>> getVisibleDimensions(ServerPlayer player) {
        return getPlayerInfo(player).visibleDimensions;
    }
    
    public static void syncBlockUpdateToClientImmediately(ServerLevel world, IntBox box) {
        ChunkPos lowPos = new ChunkPos(box.l);
        ChunkPos highPos = new ChunkPos(box.h);
        
        // flush pending-sending chunks
        Set<ServerPlayer> playersViewingRegion = new HashSet<>();
        ResourceKey<Level> dimension = world.dimension();
        for (int x = lowPos.x; x <= highPos.x; x++) {
            for (int z = lowPos.z; z <= highPos.z; z++) {
                Object2ObjectOpenHashMap<ServerPlayer, PlayerWatchRecord> rec =
                    getWatchRecordForChunk(dimension, x, z);
                if (rec != null) {
                    for (PlayerWatchRecord r : rec.values()) {
                        if (r.isValid && !r.isLoadedToPlayer) {
                            playersViewingRegion.add(r.player);
                        }
                    }
                }
            }
        }
        for (ServerPlayer player : playersViewingRegion) {
            getPlayerInfo(player).doChunkSending(player);
        }
        
        IEChunkMap chunkMap = (IEChunkMap) world.getChunkSource().chunkMap;
        
        for (int x = lowPos.x; x <= highPos.x; x++) {
            for (int z = lowPos.z; z <= highPos.z; z++) {
                long chunkPosLong = ChunkPos.asLong(x, z);
                
                ChunkHolder chunkHolder = chunkMap.ip_getChunkHolder(chunkPosLong);
                if (chunkHolder != null) {
                    LevelChunk tickingChunk = chunkHolder.getTickingChunk();
                    if (tickingChunk != null) {
                        chunkHolder.broadcastChanges(tickingChunk);
                    }
                }
            }
        }
    }
    
    public static class RemoteCallables {
        public static void acceptClientPerformanceInfo(
            ServerPlayer player,
            PerformanceLevel performanceLevel
        ) {
            PlayerChunkLoading playerInfo = getPlayerInfo(player);
            playerInfo.performanceLevel = performanceLevel;
        }
    }
}
