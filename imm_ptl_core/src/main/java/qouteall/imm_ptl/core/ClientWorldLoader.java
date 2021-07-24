package qouteall.imm_ptl.core;

import qouteall.imm_ptl.core.dimension_sync.DimensionTypeSync;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.SignalArged;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.DimensionRenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    // sent to client by login and respawn packets
    public static boolean isFlatWorld = false;
    
    private static final Map<RegistryKey<World>, ClientWorld> clientWorldMap = new HashMap<>();
    public static final Map<RegistryKey<World>, WorldRenderer> worldRendererMap = new HashMap<>();
    public static final Map<RegistryKey<World>, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    private static boolean isInitialized = false;
    
    private static boolean isCreatingClientWorld = false;
    
    public static boolean isClientRemoteTicking = false;
    
    public static final SignalArged<ClientWorld> clientWorldLoadSignal = new SignalArged<>();
    
    public static void init() {
        IPGlobal.postClientTickSignal.connect(ClientWorldLoader::tick);
        
        IPGlobal.clientCleanupSignal.connect(ClientWorldLoader::cleanUp);
    }
    
    public static boolean getIsInitialized() {
        return isInitialized;
    }
    
    public static boolean getIsCreatingClientWorld() {
        return isCreatingClientWorld;
    }
    
    private static void tick() {
        if (IPCGlobal.isClientRemoteTickingEnabled) {
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
        ((IEParticleManager) client.particleManager).ip_setWorld(newWorld);
        
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
            ((IEParticleManager) client.particleManager).ip_setWorld(oldWorld);
            ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        }
    }
    
    // show nether particles through portal
    // TODO optimize it
    private static void tickRemoteWorldRandomTicksClient(
        ClientWorld newWorld, List<Portal> nearbyPortals
    ) {
        if (newWorld.getTime() % 3 == 0) {
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
    }
    
    private static void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> {
                worldRenderer.setWorld(null);
                if (worldRenderer != client.worldRenderer) {
                    worldRenderer.close();
                    ((IEWorldRenderer) worldRenderer).portal_fullyDispose();
                }
            }
        );
        
        for (ClientWorld clientWorld : clientWorldMap.values()) {
            ((IEClientWorld) clientWorld).resetWorldRendererRef();
        }
        
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
    
    public static void initializeIfNeeded() {
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
            //multiple net handlers share the same playerListEntries object
            ClientPlayNetworkHandler mainNetHandler = client.player.networkHandler;
            
            RegistryKey<DimensionType> dimensionTypeKey =
                DimensionTypeSync.getDimensionTypeKey(dimension);
            ClientWorld.Properties currentProperty =
                (ClientWorld.Properties) ((IEWorld) client.world).myGetProperties();
            DynamicRegistryManager dimensionTracker = mainNetHandler.getRegistryManager();
            
            DimensionType dimensionType = dimensionTracker
                .get(Registry.DIMENSION_TYPE_KEY).get(dimensionTypeKey);
            
            ClientWorld.Properties properties = new ClientWorld.Properties(
                currentProperty.getDifficulty(),
                currentProperty.isHardcore(),
                isFlatWorld
            );
            newWorld = new ClientWorld(
                mainNetHandler,
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
        
        // there are two "reload" methods
        worldRenderer.reload(client.getResourceManager());
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Client World Created " + dimension.getValue());
        
        isCreatingClientWorld = false;
        
        clientWorldLoadSignal.emit(newWorld);
        
        client.getProfiler().pop();
        
        return newWorld;
    }
    
    public static Collection<ClientWorld> getClientWorlds() {
        Validate.isTrue(isInitialized);
        
        return clientWorldMap.values();
    }
    
}
