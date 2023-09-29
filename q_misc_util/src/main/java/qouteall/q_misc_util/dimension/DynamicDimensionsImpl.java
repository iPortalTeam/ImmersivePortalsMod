package qouteall.q_misc_util.dimension;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscGlobals;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.MiscNetworking;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.ducks.IEMappedRegistry2;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;
import qouteall.q_misc_util.mixin.dimension.IEMappedRegistry;
import qouteall.q_misc_util.mixin.dimension.IEWorldBorder;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.q_misc_util.my_util.SignalArged;

import java.io.IOException;
import java.util.List;

public class DynamicDimensionsImpl {
    public static final SignalArged<ResourceKey<Level>> beforeRemovingDimensionSignal = new SignalArged<>();
    
    public static boolean isRemovingDimension = false;
    
    public static void init() {
    
    }
    
    public static void addDimensionDynamically(
        MinecraftServer server,
        ResourceLocation dimensionId,
        LevelStem levelStem
    ) {
        /**{@link MinecraftServer#createLevels(ChunkProgressListener)}*/
        
        ResourceKey<Level> dimensionResourceKey = DimId.idToKey(dimensionId);
        
        Validate.isTrue(server.isSameThread());
        
        if (server.getLevel(dimensionResourceKey) != null) {
            throw new RuntimeException("Dimension " + dimensionId + " already exists.");
        }
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        WorldBorder worldBorder = overworld.getWorldBorder();
        Validate.notNull(worldBorder);
        
        WorldData worldData = server.getWorldData();
        ServerLevelData serverLevelData = worldData.overworldData();
        
        long seed = worldData.worldGenOptions().seed();
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
            levelStem,
            new DummyProgressListener(),
            false, // isDebug
            obfuscatedSeed,
            ImmutableList.of(),
            false, // only true for overworld
            overworld.getRandomSequences()
        );
        
        worldBorder.addListener(
            new BorderChangeListener.DelegateBorderChangeListener(newWorld.getWorldBorder())
        );
        
        ((IEMinecraftServer_Misc) server).ip_addDimensionToWorldMap(dimensionResourceKey, newWorld);
        
        /**
         * register it into registry, so it will be saved in
         * {@link WorldGenSettings#encode(DynamicOps, WorldOptions, RegistryAccess)} ,
         * so it will be saved into level.dat
         * */
        Registry<LevelStem> levelStemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        ((IEMappedRegistry) levelStemRegistry).ip_setIsFrozen(false);
        ((MappedRegistry<LevelStem>) levelStemRegistry).register(
            ResourceKey.create(Registries.LEVEL_STEM, dimensionId),
            levelStem, Lifecycle.stable()
        );
        ((IEMappedRegistry) levelStemRegistry).ip_setIsFrozen(true);
        
        worldBorder.applySettings(serverLevelData.getWorldBorder());
        
        Helper.log("Added Dimension " + dimensionId);
        
        DimensionIdManagement.updateAndSaveServerDimIdRecord();
        
        Packet dimSyncPacket = MiscNetworking.DimSyncPacket.createPacket(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(dimSyncPacket);
        }
        
        DimensionAPI.serverDimensionDynamicUpdateEvent.invoker().run(server.levelKeys());
    }
    
    public static void removeDimensionDynamically(ServerLevel world) {
        MinecraftServer server = world.getServer();
        
        Validate.isTrue(server.isSameThread());
        
        ResourceKey<Level> dimension = world.dimension();
        
        if (dimension == Level.OVERWORLD || dimension == Level.NETHER || dimension == Level.END) {
            throw new RuntimeException();
        }
        
        Helper.log("Started Removing Dimension " + dimension.location());
        
        MiscGlobals.serverTaskList.addTask(MyTaskList.oneShotTask(() -> {
            beforeRemovingDimensionSignal.emit(dimension);
            
            evacuatePlayersFromDimension(world);
            
            /**{@link MinecraftServer#stopServer()}*/
            
            long startTime = System.nanoTime();
            long lastLogTime = System.nanoTime();
            
            isRemovingDimension = true;
            
            ((IEMinecraftServer_Misc) server).ip_removeDimensionFromWorldMap(dimension);
            
            try {
                while (world.getChunkSource().chunkMap.hasWork()) {
                    world.getChunkSource().removeTicketsOnClosing();
                    world.getChunkSource().tick(() -> true, false);
                    world.getChunkSource().pollTask();
                    server.pollTask();
                    
                    if (System.nanoTime() - lastLogTime > Helper.secondToNano(1)) {
                        lastLogTime = System.nanoTime();
                        Helper.log("waiting for chunk tasks to finish");
                    }
                    
                    if (System.nanoTime() - startTime > Helper.secondToNano(15)) {
                        Helper.err("Waited too long for chunk tasks");
                        break;
                    }
                    
                    ((IEMinecraftServer_Misc) server).ip_waitUntilNextTick();
                }
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
            
            isRemovingDimension = false;
            
            Helper.log("Finished chunk tasks in %f seconds"
                .formatted(Helper.nanoToSecond(System.nanoTime() - startTime))
            );
            
            Helper.log("Chunk num:%d Has entities:%s".formatted(
                world.getChunkSource().chunkMap.size(),
                world.getAllEntities().iterator().hasNext()
            ));
            
            server.saveAllChunks(false, true, false);
            
            try {
                world.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
            resetWorldBorderListener(server);
            
            // force remove it from registry, so it will not be saved into level.dat
            Registry<LevelStem> levelStemRegistry = server.registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM);
            ((IEMappedRegistry2) levelStemRegistry).ip_forceRemove(dimension.location());
            
            Helper.log("Successfully Removed Dimension " + dimension.location());
            
            Packet dimSyncPacket = MiscNetworking.DimSyncPacket.createPacket(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(dimSyncPacket);
            }
            
            DimensionAPI.serverDimensionDynamicUpdateEvent.invoker().run(server.levelKeys());
        }));
    }
    
    private static void resetWorldBorderListener(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        WorldBorder worldBorder = overworld.getWorldBorder();
        List<BorderChangeListener> borderChangeListeners = ((IEWorldBorder) worldBorder).ip_getListeners();
        borderChangeListeners.clear();
        for (ServerLevel serverWorld : server.getAllLevels()) {
            if (serverWorld != overworld) {
                worldBorder.addListener(
                    new BorderChangeListener.DelegateBorderChangeListener(serverWorld.getWorldBorder())
                );
            }
        }
        server.getPlayerList().addWorldborderListener(overworld);
    }
    
    private static void evacuatePlayersFromDimension(ServerLevel world) {
        MinecraftServer server = MiscHelper.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        
        List<ServerPlayer> players = world.getPlayers(p -> true);
        
        BlockPos sharedSpawnPos = overworld.getSharedSpawnPos();
        
        for (ServerPlayer player : players) {
            player.teleportTo(
                overworld,
                sharedSpawnPos.getX(), sharedSpawnPos.getY(), sharedSpawnPos.getZ(),
                0, 0
            );
            player.sendSystemMessage(
                Component.literal(
                    "Teleported to spawn pos because dimension %s had been removed"
                        .formatted(world.dimension().location())
                )
            );
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
