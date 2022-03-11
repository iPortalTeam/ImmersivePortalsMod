package qouteall.q_misc_util.dimension;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.MiscNetworking;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.dimension.DimensionIdManagement;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;
import qouteall.q_misc_util.my_util.SignalArged;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class DynamicDimensionsImpl {
    public static final SignalArged<ResourceKey<Level>> removeDimensionSignal = new SignalArged<>();
    
    private static WeakHashMap<ServerLevel, WeakReference<BorderChangeListener>> worldBorderListenerMap
        = new WeakHashMap<>();
    
    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> worldBorderListenerMap.clear());
    }
    
    public static void addDimensionDynamically(
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        /**{@link MinecraftServer#createLevels(ChunkProgressListener)}*/
        
        MinecraftServer server = MiscHelper.getServer();
        ResourceKey<Level> dimensionResourceKey = DimId.idToKey(dimensionId);
        
        if (server.getLevel(dimensionResourceKey) != null) {
            throw new RuntimeException("Dimension " + dimensionId + " already exists.");
        }
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        WorldBorder worldBorder = overworld.getWorldBorder();
        Validate.notNull(worldBorder);
        
        WorldData worldData = server.getWorldData();
        ServerLevelData serverLevelData = worldData.overworldData();
        
        WorldGenSettings worldGenSettings = worldData.worldGenSettings();
        
        long seed = worldGenSettings.seed();
        long obfuscatedSeed = BiomeManager.obfuscateSeed(seed);
        
        DerivedLevelData derivedLevelData = new DerivedLevelData(
            worldData, serverLevelData
        );
        
        ServerLevel newWorld = new ServerLevel(
            server,
            ((IEMinecraftServer_Misc) server).ip_getExecutor(),
            ((IEMinecraftServer_Misc) server).ip_getStorageSource(),
            derivedLevelData,
            dimensionResourceKey,
            levelStem.typeHolder(),
            new DummyProgressListener(),
            levelStem.generator(),
            false, // isDebug
            obfuscatedSeed,
            ImmutableList.of(),
            false // only true for overworld
        );
        
        worldBorder.addListener(
            new BorderChangeListener.DelegateBorderChangeListener(newWorld.getWorldBorder())
        );
        
        ((IEMinecraftServer_Misc) server).addDimensionToWorldMap(dimensionResourceKey, newWorld);
        
        worldBorder.applySettings(serverLevelData.getWorldBorder());
        
        // don't save it into level.dat
//        Registry.register(
//            worldGenSettings.dimensions(),
//            dimensionId,
//            levelStem
//        );
        
        DimensionIdManagement.updateAndSaveServerDimIdRecord();
        
        Packet dimSyncPacket = MiscNetworking.createDimSyncPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(dimSyncPacket);
        }
    }
    
    public static void removeDimensionDynamically(ServerLevel world) {
        MinecraftServer server = MiscHelper.getServer();
        
        ResourceKey<Level> dimension = world.dimension();
        
        if (dimension == Level.OVERWORLD || dimension == Level.NETHER || dimension == Level.END) {
            throw new RuntimeException();
        }
        
        removeDimensionSignal.emit(dimension);
        
        /**{@link MinecraftServer#stopServer()}*/
        
        while (world.getChunkSource().chunkMap.hasWork()) {
            world.getChunkSource().removeTicketsOnClosing();
            world.getChunkSource().tick(() -> true, false);
        }
        
        server.saveAllChunks(false, true, false);
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        WorldBorder worldBorder = overworld.getWorldBorder();
        
        WeakReference<BorderChangeListener> wr = worldBorderListenerMap.get(world);
        if (wr != null) {
            BorderChangeListener borderChangeListener = wr.get();
            if (borderChangeListener != null) {
                worldBorder.removeListener(borderChangeListener);
            }
        }
        
        try {
            world.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        ((IEMinecraftServer_Misc) server).removeDimensionFromWorldMap(dimension);
        
        Packet dimSyncPacket = MiscNetworking.createDimSyncPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(dimSyncPacket);
        }
        
    }
    
    private static class DummyProgressListener implements ChunkProgressListener {
        
        @Override
        public void updateSpawnPos(ChunkPos center) {
        
        }
        
        @Override
        public void onStatusChange(ChunkPos chunkPosition, @Nullable ChunkStatus newStatus) {
        
        }
        
        @Override
        public void start() {
        
        }
        
        @Override
        public void stop() {
        
        }
    }
    
}
