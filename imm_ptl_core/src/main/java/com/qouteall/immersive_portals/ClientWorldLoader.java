package com.qouteall.immersive_portals;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEWorld;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    private static final Map<RegistryKey<World>, ClientWorld> clientWorldMap = new HashMap<>();
    public static final Map<RegistryKey<World>, WorldRenderer> worldRendererMap = new HashMap<>();
    public static final Map<RegistryKey<World>, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    private static boolean isInitialized = false;
    
    private static boolean isCreatingClientWorld = false;
    
    public static boolean isClientRemoteTicking = false;
    
    public static void init() {
        ModMain.postClientTickSignal.connect(ClientWorldLoader::tick);
        
        ModMain.clientCleanupSignal.connect(ClientWorldLoader::cleanUp);
    }
    
    public static boolean getIsInitialized() {
        return isInitialized;
    }
    
    public static boolean getIsCreatingClientWorld() {
        return isCreatingClientWorld;
    }
    
    private static void tick() {
        if (CGlobal.isClientRemoteTickingEnabled) {
            isClientRemoteTicking = true;
            clientWorldMap.values().forEach(world -> {
                if (client.world != world) {
                    tickRemoteWorld(world);
                }
            });
            worldRendererMap.values().forEach(worldRenderer -> {
                if (worldRenderer != client.worldRenderer) {
                    worldRenderer.tick();
                }
            });
            isClientRemoteTicking = false;
        }
        
        boolean lightmapTextureConflict = false;
        for (DimensionRenderHelper helper : renderHelperMap.values()) {
            helper.tick();
            if (helper.world != client.world) {
                if (helper.lightmapTexture == client.gameRenderer.getLightmapTextureManager()) {
                    Helper.err(String.format(
                        "Lightmap Texture Conflict %s %s",
                        helper.world.getRegistryKey(),
                        client.world.getRegistryKey()
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
        
    }
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    private static void tickRemoteWorld(ClientWorld newWorld) {
        List<Portal> nearbyPortals = CHelper.getClientNearbyPortals(10).collect(Collectors.toList());
        
        WorldRenderer newWorldRenderer = getWorldRenderer(newWorld.getRegistryKey());
        
        ClientWorld oldWorld = client.world;
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        
        client.world = newWorld;
        ((IEParticleManager) client.particleManager).mySetWorld(newWorld);
        
        //the world renderer's world field is used for particle spawning
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        
        try {
            newWorld.tickEntities();
            newWorld.tick(() -> true);
            
            if (!client.isPaused()) {
                tickRemoteWorldRandomTicksClient(newWorld, nearbyPortals);
            }
        }
        catch (Throwable e) {
            limitedLogger.invoke(e::printStackTrace);
        }
        finally {
            client.world = oldWorld;
            ((IEParticleManager) client.particleManager).mySetWorld(oldWorld);
            ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        }
    }
    
    // show nether particles through portal
    // TODO optimize it
    private static void tickRemoteWorldRandomTicksClient(
        ClientWorld newWorld, List<Portal> nearbyPortals
    ) {
        nearbyPortals.stream().filter(
            portal -> portal.dimensionTo == newWorld.getRegistryKey()
        ).findFirst().ifPresent(portal -> {
            Vec3d playerPos = client.player.getPos();
            Vec3d center = portal.transformPoint(playerPos);
            
            Camera camera = client.gameRenderer.getCamera();
            Vec3d oldCameraPos = camera.getPos();
            
            ((IECamera) camera).portal_setPos(center);
            
            newWorld.doRandomBlockDisplayTicks(
                (int) center.x, (int) center.y, (int) center.z
            );
            
            client.particleManager.tick();
            
            ((IECamera) camera).portal_setPos(oldCameraPos);
        });
    }
    
    private static void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> worldRenderer.setWorld(null)
        );
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        
        renderHelperMap.values().forEach(DimensionRenderHelper::cleanUp);
        renderHelperMap.clear();
        
        isInitialized = false;
        
        
    }
    
    //@Nullable
    public static WorldRenderer getWorldRenderer(RegistryKey<World> dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    public static ClientWorld getWorld(RegistryKey<World> dimension) {
        Validate.notNull(dimension);
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createSecondaryClientWorld(dimension);
        }
        
        return clientWorldMap.get(dimension);
    }
    
    public static DimensionRenderHelper getDimensionRenderHelper(RegistryKey<World> dimension) {
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
    
    private static void initializeIfNeeded() {
        if (!isInitialized) {
            Validate.isTrue(client.world != null);
            Validate.isTrue(client.worldRenderer != null);
            
            Validate.notNull(client.player);
            Validate.isTrue(client.player.world == client.world);
            
            RegistryKey<World> playerDimension = client.world.getRegistryKey();
            clientWorldMap.put(playerDimension, client.world);
            worldRendererMap.put(playerDimension, client.worldRenderer);
            renderHelperMap.put(
                client.world.getRegistryKey(),
                new DimensionRenderHelper(client.world)
            );
            
            isInitialized = true;
        }
    }
    
    private static ClientWorld createSecondaryClientWorld(RegistryKey<World> dimension) {
        Validate.isTrue(client.player.world.getRegistryKey() != dimension);
        
        isCreatingClientWorld = true;
        
        client.getProfiler().push("create_world");
        
        int chunkLoadDistance = 3;// my own chunk manager doesn't need it
        
        WorldRenderer worldRenderer = new WorldRenderer(client, client.getBufferBuilders());
        
        ClientWorld newWorld;
        try {
            ClientPlayNetworkHandler newNetworkHandler = new ClientPlayNetworkHandler(
                client,
                new ChatScreen("You should not be seeing me. I'm just a faked screen."),
                new ClientConnection(NetworkSide.CLIENTBOUND),
                new GameProfile(null, "faked_profiler_id")
            );
            //multiple net handlers share the same playerListEntries object
            ClientPlayNetworkHandler mainNetHandler = client.player.networkHandler;
            ((IEClientPlayNetworkHandler) newNetworkHandler).setPlayerListEntries(
                ((IEClientPlayNetworkHandler) mainNetHandler).getPlayerListEntries()
            );
            RegistryKey<DimensionType> dimensionTypeKey =
                DimensionTypeSync.getDimensionTypeKey(dimension);
            ClientWorld.Properties currentProperty =
                (ClientWorld.Properties) ((IEWorld) client.world).myGetProperties();
            DynamicRegistryManager dimensionTracker = mainNetHandler.getRegistryManager();
            ((IEClientPlayNetworkHandler) newNetworkHandler).portal_setRegistryManager(
                dimensionTracker);
            DimensionType dimensionType = dimensionTracker
                .getDimensionTypes().get(dimensionTypeKey);
            
            ClientWorld.Properties properties = new ClientWorld.Properties(
                currentProperty.getDifficulty(),
                currentProperty.isHardcore(),
                currentProperty.getSkyDarknessHeight() < 1.0
            );
            newWorld = new ClientWorld(
                newNetworkHandler,
                properties,
                dimension,
                dimensionType,
                chunkLoadDistance,
                () -> client.getProfiler(),
                worldRenderer,
                client.world.isDebugWorld(),
                client.world.getBiomeAccess().seed
            );
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Client World " + dimension + " " + clientWorldMap.keySet(),
                e
            );
        }
        
        worldRenderer.setWorld(newWorld);
        
        worldRenderer.apply(client.getResourceManager());
        
        ((IEClientPlayNetworkHandler) ((IEClientWorld) newWorld).getNetHandler())
            .setWorld(newWorld);
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Client World Created " + dimension.getValue());
        
        isCreatingClientWorld = false;
        
        client.getProfiler().pop();
        
        return newWorld;
    }
    
    public static Collection<ClientWorld> getClientWorlds() {
        Validate.isTrue(isInitialized);
        
        return clientWorldMap.values();
    }
    
    @Nullable
    public static Vec3d getTransformedSoundPosition(
        ClientWorld soundWorld,
        Vec3d soundPos
    ) {
        return McHelper.getNearbyPortals(
            soundWorld, soundPos, 10
        ).filter(
            portal -> portal.getDestDim() == RenderStates.originalPlayerDimension &&
                portal.transformPoint(soundPos).distanceTo(RenderStates.originalPlayerPos) < 20
        ).findFirst().map(
            portal -> portal.transformPoint(soundPos)
        ).orElse(null);
    }
}
