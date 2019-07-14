package com.qouteall.immersive_portals.client_world_management;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.render.DimensionRenderHelper;
import com.sun.istack.internal.Nullable;
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

public class ClientWorldLoader {
    public Map<DimensionType, ClientWorld> clientWorldMap = new HashMap<>();
    public Map<DimensionType, WorldRenderer> worldRendererMap = new HashMap<>();
    public Map<DimensionType, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private MinecraftClient mc = MinecraftClient.getInstance();
    
    private boolean isInitialized = false;
    
    private boolean isLoadingFakedWorld = false;
    
    //TODO release it
    public boolean isClientRemoteTickingEnabled = false;
    
    public ClientWorldLoader() {
        ModMain.postClientTickSignal.connectWithWeakRef(this, ClientWorldLoader::tick);
    }
    
    public boolean getIsLoadingFakedWorld() {
        return isLoadingFakedWorld;
    }
    
    public DimensionType getPlayerDimension() {
        return mc.world.getDimension().getType();
    }
    
    private void initialize() {
        assert (mc.world != null);
        assert (mc.worldRenderer != null);
        
        clientWorldMap.put(getPlayerDimension(), mc.world);
        worldRendererMap.put(getPlayerDimension(), mc.worldRenderer);
        
        isInitialized = true;
    }
    
    private void tick() {
        if (isClientRemoteTickingEnabled) {
            clientWorldMap.values().forEach(ClientWorld -> {
                if (mc.world != ClientWorld) {
                    //NOTE tick() does not include ticking entities
                    ClientWorld.tickEntities();
                    ClientWorld.tick(() -> true);
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
    
    @Nullable
    public ClientWorld getDimension(DimensionType dimension) {
        initializeIfNeeded();
        
        return clientWorldMap.get(dimension);
    }
    
    @Nullable
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
        return renderHelperMap.computeIfAbsent(
            dimension,
            dimensionType -> new DimensionRenderHelper(
                getOrCreateFakedWorld(dimension)
            )
        );
    }
    
    private void initializeIfNeeded() {
        if (isInitialized == false) {
            initialize();
        }
    }
    
    private ClientWorld createFakedClientWorld(DimensionType dimension) {
        assert (mc.player.dimension != dimension);
        
        isLoadingFakedWorld = true;
        
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
                false,
                LevelGeneratorType.FLAT
            ),
            dimension,
            chunkLoadDistance,
            mc.getProfiler(),
            worldRenderer
        );
        
        worldRenderer.setWorld(newWorld);
        
        ((IEClientPlayNetworkHandler) ((IEClientWorld) newWorld).getNetHandler())
            .setWorld(newWorld);
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Faked World Created " + dimension);
        
        isLoadingFakedWorld = false;
        
        return newWorld;
    }
    
}
