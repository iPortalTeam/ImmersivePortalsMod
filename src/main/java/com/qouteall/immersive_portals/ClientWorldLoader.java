package com.qouteall.immersive_portals;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
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
import org.apache.commons.lang3.Validate;

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
    public ClientWorld getWorld(DimensionType dimension) {
        initializeIfNeeded();
        
        return clientWorldMap.get(dimension);
    }
    
    //@Nullable
    public WorldRenderer getWorldRenderer(DimensionType dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    public ClientWorld getOrCreateFakedWorld(DimensionType dimension) {
        Validate.notNull(dimension);
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createFakedClientWorld(dimension);
        }
    
        return getWorld(dimension);
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
            newWorld = new ClientWorld(
                newNetworkHandler,
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
    
        OFHelper.onFinishCreatingFakedWorld();
        
        return newWorld;
    }
    
}
