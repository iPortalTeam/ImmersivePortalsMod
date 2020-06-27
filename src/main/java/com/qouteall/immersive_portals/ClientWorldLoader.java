package com.qouteall.immersive_portals;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEWorld;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    public final Map<RegistryKey<World>, ClientWorld> clientWorldMap = new HashMap<>();
    public final Map<RegistryKey<World>, WorldRenderer> worldRendererMap = new HashMap<>();
    public final Map<RegistryKey<World>, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    private Set<DimensionalChunkPos> unloadedChunks = new HashSet<>();
    
    private MinecraftClient mc = MinecraftClient.getInstance();
    
    private boolean isInitialized = false;
    
    private boolean isLoadingFakedWorld = false;
    
    private boolean isHardCore = false;
    
    public boolean isClientRemoteTicking = false;
    
    public int ticksSinceEnteringWorld = 0;
    
    public ClientWorldLoader() {
        ModMain.postClientTickSignal.connectWithWeakRef(this, ClientWorldLoader::tick);
    }
    
    public boolean getIsLoadingFakedWorld() {
        return isLoadingFakedWorld;
    }
    
    private void tick() {
        if (CGlobal.isClientRemoteTickingEnabled) {
            isClientRemoteTicking = true;
            clientWorldMap.values().forEach(world -> {
                if (mc.world != world) {
                    tickRemoteWorld(world);
                }
            });
            worldRendererMap.values().forEach(worldRenderer -> {
                if (worldRenderer != mc.worldRenderer) {
                    worldRenderer.tick();
                }
            });
            isClientRemoteTicking = false;
        }
        
        boolean lightmapTextureConflict = false;
        for (DimensionRenderHelper helper : renderHelperMap.values()) {
            helper.tick();
            if (helper.world != mc.world) {
                if (helper.lightmapTexture == mc.gameRenderer.getLightmapTextureManager()) {
                    Helper.err(String.format(
                        "Lightmap Texture Conflict %s %s",
                        helper.world.getRegistryKey(),
                        mc.world.getRegistryKey()
                    ));
                    lightmapTextureConflict = true;
                }
            }
        }
        if (lightmapTextureConflict) {
            renderHelperMap.values().forEach(DimensionRenderHelper::cleanUp);
            renderHelperMap.clear();
            Helper.log("Refreshed Lightmaps");
        }
        ticksSinceEnteringWorld++;
    }
    
    private static int reportedErrorNum = 0;
    
    private void tickRemoteWorld(ClientWorld newWorld) {
        ClientWorld oldWorld = mc.world;
        
        mc.world = newWorld;
        ((IEParticleManager) mc.particleManager).mySetWorld(newWorld);
        
        try {
            newWorld.tickEntities();
            newWorld.tick(() -> true);
        }
        catch (Throwable e) {
            if (reportedErrorNum < 200) {
                e.printStackTrace();
                reportedErrorNum++;
            }
        }
        finally {
            mc.world = oldWorld;
            ((IEParticleManager) mc.particleManager).mySetWorld(oldWorld);
        }
    }
    
    public void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> worldRenderer.setWorld(null)
        );
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        
        renderHelperMap.values().forEach(DimensionRenderHelper::cleanUp);
        renderHelperMap.clear();
        
        isInitialized = false;
        
        ModMain.clientTaskList.forceClearTasks();
        
        ticksSinceEnteringWorld = 0;
    }
    
    //@Nullable
    public WorldRenderer getWorldRenderer(RegistryKey<World> dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    //Create world if missing
    public ClientWorld getWorld(RegistryKey<World> dimension) {
        Validate.notNull(dimension);
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createFakedClientWorld(dimension);
        }
        
        return clientWorldMap.get(dimension);
    }
    
    public DimensionRenderHelper getDimensionRenderHelper(RegistryKey<World> dimension) {
        initializeIfNeeded();
        
        DimensionRenderHelper result = renderHelperMap.computeIfAbsent(
            dimension,
            dimensionType -> {
                return new DimensionRenderHelper(
                    getWorld(dimension)
                );
            }
        );
        
        Validate.isTrue(result.world.getRegistryKey() == dimension);
        
        return result;
    }
    
    private void initializeIfNeeded() {
        if (!isInitialized) {
            assert (mc.world != null);
            assert (mc.worldRenderer != null);
            
            RegistryKey<World> playerDimension = mc.world.getRegistryKey();
            clientWorldMap.put(playerDimension, mc.world);
            worldRendererMap.put(playerDimension, mc.worldRenderer);
            renderHelperMap.put(
                mc.world.getRegistryKey(),
                new DimensionRenderHelper(mc.world)
            );
            
            isHardCore = mc.world.getLevelProperties().isHardcore();
            
            isInitialized = true;
        }
    }
    
    //fool minecraft using the faked world
    private ClientWorld createFakedClientWorld(RegistryKey<World> dimension) {
        Validate.isTrue(mc.player.world.getRegistryKey() != dimension);
        
        isLoadingFakedWorld = true;
        
        //TODO get load distance
        int chunkLoadDistance = 3;
        
        WorldRenderer worldRenderer = new WorldRenderer(mc, mc.getBufferBuilders());
        
        ClientWorld newWorld;
        try {
            ClientPlayNetworkHandler newNetworkHandler = new ClientPlayNetworkHandler(
                mc,
                new ChatScreen("You should not be seeing me. I'm just a faked screen."),
                new ClientConnection(NetworkSide.CLIENTBOUND),
                new GameProfile(null, "faked_profiler_id")
            );
            //multiple net handlers share the same playerListEntries object
            ((IEClientPlayNetworkHandler) newNetworkHandler).setPlayerListEntries(
                ((IEClientPlayNetworkHandler) mc.player.networkHandler).getPlayerListEntries()
            );
            RegistryKey<DimensionType> dimensionTypeKey =
                DimensionTypeSync.getDimensionTypeKey(dimension);
            ClientWorld.Properties currentProperty =
                (ClientWorld.Properties) ((IEWorld) mc.world).myGetProperties();
            newWorld = new ClientWorld(
                newNetworkHandler,
                new ClientWorld.Properties(
                    currentProperty.getDifficulty(), currentProperty.isHardcore(),
                    currentProperty.getSkyDarknessHeight() < 1.0
                ),
                dimension,
                dimensionTypeKey,
                DimensionTypeSync.getDimensionType(dimensionTypeKey),
                chunkLoadDistance,
                () -> mc.getProfiler(),
                worldRenderer,
                mc.world.isDebugWorld(),
                23333333
            );
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Faked World " + dimension + " " + clientWorldMap.keySet(),
                e
            );
        }
        
        worldRenderer.setWorld(newWorld);
        
        worldRenderer.apply(mc.getResourceManager());
        
        ((IEClientPlayNetworkHandler) ((IEClientWorld) newWorld).getNetHandler())
            .setWorld(newWorld);
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Faked World Created " + dimension);
        
        isLoadingFakedWorld = false;
        
        return newWorld;
    }
    
}
