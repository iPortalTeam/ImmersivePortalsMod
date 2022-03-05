package qouteall.imm_ptl.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.dimension_sync.DimensionTypeSync;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.network.IPCommonNetworkClient;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.DimensionRenderHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    // sent to client by login and respawn packets
    public static boolean isFlatWorld = false;
    
    private static final Map<ResourceKey<Level>, ClientLevel> clientWorldMap = new HashMap<>();
    public static final Map<ResourceKey<Level>, LevelRenderer> worldRendererMap = new HashMap<>();
    public static final Map<ResourceKey<Level>, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private static final Minecraft client = Minecraft.getInstance();
    
    private static boolean isInitialized = false;
    
    private static boolean isCreatingClientWorld = false;
    
    public static boolean isClientRemoteTicking = false;
    
    public static final SignalArged<ClientLevel> clientWorldLoadSignal = new SignalArged<>();
    
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
                if (client.level != world) {
                    tickRemoteWorld(world);
                }
            });
            worldRendererMap.values().forEach(worldRenderer -> {
                if (worldRenderer != client.levelRenderer) {
                    worldRenderer.tick();
                }
            });
            isClientRemoteTicking = false;
        }
        
        boolean lightmapTextureConflict = false;
        for (DimensionRenderHelper helper : renderHelperMap.values()) {
            helper.tick();
            if (helper.world != client.level) {
                if (helper.lightmapTexture == client.gameRenderer.lightTexture()) {
                    Helper.err(String.format(
                        "Lightmap Texture Conflict %s %s",
                        helper.world.dimension(),
                        client.level.dimension()
                    ));
                    lightmapTextureConflict = true;
                }
            }
        }
        if (lightmapTextureConflict) {
            disposeRenderHelpers();
            Helper.log("Refreshed Lightmaps");
        }
        
    }
    
    public static void disposeRenderHelpers() {
        renderHelperMap.values().forEach(DimensionRenderHelper::cleanUp);
        renderHelperMap.clear();
    }
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    private static void tickRemoteWorld(ClientLevel newWorld) {
        List<Portal> nearbyPortals = CHelper.getClientNearbyPortals(10).collect(Collectors.toList());
        
        IPCommonNetworkClient.withSwitchedWorld(newWorld, () -> {
            try {
                newWorld.tickEntities();
                newWorld.tick(() -> true);
                
                if (!client.isPaused()) {
                    tickRemoteWorldRandomTicksClient(newWorld, nearbyPortals);
                }
                
                newWorld.pollLightUpdates();
            }
            catch (Throwable e) {
                limitedLogger.invoke(e::printStackTrace);
            }
        });
    }
    
    // show nether particles through portal
    // TODO optimize it
    private static void tickRemoteWorldRandomTicksClient(
        ClientLevel newWorld, List<Portal> nearbyPortals
    ) {
        nearbyPortals.stream().filter(
            portal -> portal.dimensionTo == newWorld.dimension()
        ).findFirst().ifPresent(portal -> {
            Vec3 playerPos = client.player.position();
            Vec3 center = portal.transformPoint(playerPos);
            
            Camera camera = client.gameRenderer.getMainCamera();
            Vec3 oldCameraPos = camera.getPosition();
            
            ((IECamera) camera).portal_setPos(center);
            
            if (newWorld.getGameTime() % 2 == 0) {
                // it costs some CPU time
                newWorld.animateTick(
                    (int) center.x, (int) center.y, (int) center.z
                );
            }
            
            client.particleEngine.tick();
            
            ((IECamera) camera).portal_setPos(oldCameraPos);
        });
        
        
    }
    
    private static void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> {
                worldRenderer.setLevel(null);
                if (worldRenderer != client.levelRenderer) {
                    worldRenderer.close();
                    ((IEWorldRenderer) worldRenderer).portal_fullyDispose();
                }
            }
        );
        
        for (ClientLevel clientWorld : clientWorldMap.values()) {
            ((IEClientWorld) clientWorld).resetWorldRendererRef();
        }
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        
        disposeRenderHelpers();
        
        isInitialized = false;
        
        
    }
    
    //@Nullable
    public static LevelRenderer getWorldRenderer(ResourceKey<Level> dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    public static ClientLevel getWorld(ResourceKey<Level> dimension) {
        Validate.notNull(dimension);
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createSecondaryClientWorld(dimension);
        }
        
        return clientWorldMap.get(dimension);
    }
    
    public static DimensionRenderHelper getDimensionRenderHelper(ResourceKey<Level> dimension) {
        initializeIfNeeded();
        
        DimensionRenderHelper result = renderHelperMap.computeIfAbsent(
            dimension,
            dimensionType -> {
                return new DimensionRenderHelper(
                    getWorld(dimension)
                );
            }
        );
        
        Validate.isTrue(result.world.dimension() == dimension);
        
        return result;
    }
    
    public static void initializeIfNeeded() {
        if (!isInitialized) {
            Validate.isTrue(client.level != null);
            Validate.isTrue(client.levelRenderer != null);
            
            Validate.notNull(client.player);
            Validate.isTrue(client.player.level == client.level);
            
            ResourceKey<Level> playerDimension = client.level.dimension();
            clientWorldMap.put(playerDimension, client.level);
            worldRendererMap.put(playerDimension, client.levelRenderer);
            renderHelperMap.put(
                client.level.dimension(),
                new DimensionRenderHelper(client.level)
            );
            
            isInitialized = true;
        }
    }
    
    private static ClientLevel createSecondaryClientWorld(ResourceKey<Level> dimension) {
        Validate.isTrue(client.player.level.dimension() != dimension);
        
        isCreatingClientWorld = true;
        
        client.getProfiler().push("create_world");
        
        int chunkLoadDistance = 3;// my own chunk manager doesn't need it
        
        Map<String, MapItemSavedData> mapData = ((IEClientWorld) client.level).ip_getAllMapData();
        
        LevelRenderer worldRenderer = new LevelRenderer(client, client.renderBuffers());
        
        ClientLevel newWorld;
        try {
            ClientPacketListener mainNetHandler = client.player.connection;
            
            ResourceKey<DimensionType> dimensionTypeKey =
                DimensionTypeSync.getDimensionTypeKey(dimension);
            ClientLevel.ClientLevelData currentProperty =
                (ClientLevel.ClientLevelData) ((IEWorld) client.level).myGetProperties();
            RegistryAccess registryManager = mainNetHandler.registryAccess();
            int simulationDistance = client.level.getServerSimulationDistance();
            
            DimensionType dimensionType = registryManager
                .registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(dimensionTypeKey);
            Validate.notNull(dimensionType);
            
            ClientLevel.ClientLevelData properties = new ClientLevel.ClientLevelData(
                currentProperty.getDifficulty(),
                currentProperty.isHardcore(),
                isFlatWorld
            );
            newWorld = new ClientLevel(
                mainNetHandler,
                properties,
                dimension,
                dimensionType,
                chunkLoadDistance,
                simulationDistance,// seems that client world does not use this
                client::getProfiler,
                worldRenderer,
                client.level.isDebug(),
                client.level.getBiomeManager().biomeZoomSeed
            );
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Client World " + dimension + " " + clientWorldMap.keySet(),
                e
            );
        }
        
        ((IEClientWorld) newWorld).ip_addMapData(mapData);
        
        worldRenderer.setLevel(newWorld);
        
        // there are two "reload" methods
        worldRenderer.onResourceManagerReload(client.getResourceManager());
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Client World Created " + dimension.location());
        
        isCreatingClientWorld = false;
        
        clientWorldLoadSignal.emit(newWorld);
        
        client.getProfiler().pop();
        
        return newWorld;
    }
    
    public static Collection<ClientLevel> getClientWorlds() {
        Validate.isTrue(isInitialized);
        
        return clientWorldMap.values();
    }
    
}
