package com.qouteall.immersive_portals;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
import com.qouteall.immersive_portals.render.DimensionRenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    public Map<DimensionType, ClientWorld> clientWorldMap = new HashMap<>();
    public Map<DimensionType, WorldRenderer> worldRendererMap = new HashMap<>();
    public Map<DimensionType, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private MinecraftClient mc = MinecraftClient.getInstance();
    
    private boolean isInitialized = false;
    
    private boolean isLoadingFakedWorld = false;
    
    private boolean isHardCore = false;
    
    public ClientWorldLoader() {
        ModMain.postClientTickSignal.connectWithWeakRef(this, ClientWorldLoader::tick);
    }
    
    public boolean getIsLoadingFakedWorld() {
        return isLoadingFakedWorld;
    }
    
    private void tick() {
        if (CGlobal.isClientRemoteTickingEnabled) {
            clientWorldMap.values().forEach(world -> {
                if (mc.world != world) {
                    //NOTE tick() does not include ticking entities
                    world.tickEntities();
                    world.tick(() -> true);
                }
            });
        }
        renderHelperMap.values().forEach(DimensionRenderHelper::tick);
        
    }
    
    public void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> worldRenderer.setWorld(null)
        );
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        renderHelperMap.clear();
        
        isInitialized = false;
    }
    
    //@Nullable
    public ClientWorld getDimension(DimensionType dimension) {
        initializeIfNeeded();
        
        return clientWorldMap.get(dimension);
    }
    
    //@Nullable
    public WorldRenderer getWorldRenderer(DimensionType dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    public ClientWorld getOrCreateFakedWorld(DimensionType dimension) {
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createFakedClientWorld(dimension);
        }
        
        return getDimension(dimension);
    }
    
    public DimensionRenderHelper getDimensionRenderHelper(DimensionType dimension) {
        initializeIfNeeded();
        
        DimensionRenderHelper result = renderHelperMap.computeIfAbsent(
            dimension,
            dimensionType -> {
                return new DimensionRenderHelper(
                    getOrCreateFakedWorld(dimension)
                );
            }
        );
        assert result.world.dimension.getType() == dimension;
        return result;
    }
    
    private void initializeIfNeeded() {
        if (!isInitialized) {
            assert (mc.world != null);
            assert (mc.worldRenderer != null);
            
            DimensionType playerDimension = mc.world.getDimension().getType();
            clientWorldMap.put(playerDimension, mc.world);
            worldRendererMap.put(playerDimension, mc.worldRenderer);
            renderHelperMap.put(
                mc.world.dimension.getType(),
                new DimensionRenderHelper(mc.world)
            );
            
            isHardCore = mc.world.getLevelProperties().isHardcore();
            
            isInitialized = true;
        }
    }
    
    private ClientWorld createFakedClientWorld(DimensionType dimension) {
        assert mc.world.dimension.getType() == mc.player.dimension;
        assert (mc.player.dimension != dimension);
        
        isLoadingFakedWorld = true;
    
        OFHelper.onBeginCreatingFakedWorld();
        
        //TODO get load distance
        int chunkLoadDistance = 3;
        
        WorldRenderer worldRenderer = new WorldRenderer(mc);
        
        ClientWorld newWorld = new ClientWorld(
            new ClientPlayNetworkHandler(
                MinecraftClient.getInstance(),
                new ChatScreen("You should not be seeing me. I'm just a faked screen."),
                new ClientConnection(NetworkSide.CLIENTBOUND),
                new GameProfile(null, "faked_profiler_id")
            ),
            new LevelInfo(
                0L,
                GameMode.CREATIVE,
                true,
                isHardCore,
                LevelGeneratorType.FLAT
            ),
            dimension,
            chunkLoadDistance,
            mc.getProfiler(),
            worldRenderer
        );
        
        worldRenderer.setWorld(newWorld);
        
        worldRenderer.apply(mc.getResourceManager());
        
        ((IEClientPlayNetworkHandler) ((IEClientWorld) newWorld).getNetHandler())
            .setWorld(newWorld);
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Faked World Created " + dimension);
        
        isLoadingFakedWorld = false;
    
        OFHelper.onFinishCreatingFakedWorld();
        
        return newWorld;
    }
    
}
