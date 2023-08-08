package qouteall.imm_ptl.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.mixin.client.accessor.IEClientLevel_Accessor;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.DimensionRenderHelper;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimensionTypeSync;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientWorldLoader.class);
    
    public static final SignalArged<ResourceKey<Level>> clientDimensionDynamicRemoveSignal =
        new SignalArged<>();
    public static final SignalArged<ClientLevel> clientWorldLoadSignal = new SignalArged<>();
    
    // sent to client by login and respawn packets
    public static boolean isFlatWorld = false;
    
    private static final Map<ResourceKey<Level>, ClientLevel> clientWorldMap = new HashMap<>();
    public static final Map<ResourceKey<Level>, LevelRenderer> worldRendererMap = new HashMap<>();
    public static final Map<ResourceKey<Level>, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private static final Minecraft client = Minecraft.getInstance();
    
    private static boolean isInitialized = false;
    
    private static boolean isCreatingClientWorld = false;
    
    public static boolean isClientRemoteTicking = false;
    
    private static boolean isWorldSwitched = false;
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(ClientWorldLoader::cleanUp);
        
        DimensionAPI.clientDimensionUpdateEvent.register((serverDimensions) -> {
            if (getIsInitialized()) {
                List<ResourceKey<Level>> dimensionsToRemove =
                    clientWorldMap.keySet().stream()
                        .filter(dim -> !serverDimensions.contains(dim)).toList();
                
                for (ResourceKey<Level> dim : dimensionsToRemove) {
                    disposeDimensionDynamically(dim);
                }
                
            }
        });
    }
    
    public static boolean getIsInitialized() {
        return isInitialized;
    }
    
    public static boolean getIsCreatingClientWorld() {
        return isCreatingClientWorld;
    }
    
    public static void tick() {
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
        
        withSwitchedWorld(newWorld, () -> {
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
            ClientWorldLoader::disposeWorldRenderer
        );
        
        for (ClientLevel clientWorld : clientWorldMap.values()) {
            ((IEClientWorld) clientWorld).resetWorldRendererRef();
        }
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        
        disposeRenderHelpers();
        
        isInitialized = false;
    }
    
    private static void disposeWorldRenderer(LevelRenderer worldRenderer) {
        worldRenderer.setLevel(null);
        if (worldRenderer != client.levelRenderer) {
            worldRenderer.close();
            ((IEWorldRenderer) worldRenderer).portal_fullyDispose();
        }
    }
    
    private static void disposeDimensionDynamically(ResourceKey<Level> dimension) {
        Validate.isTrue(client.level.dimension() != dimension);
        Validate.isTrue(client.player.level().dimension() != dimension);
        Validate.isTrue(client.isSameThread());
        
        LevelRenderer worldRenderer = worldRendererMap.get(dimension);
        disposeWorldRenderer(worldRenderer);
        worldRendererMap.remove(dimension);
        
        Validate.isTrue(client.levelRenderer != worldRenderer);
        
        ClientLevel clientWorld = clientWorldMap.get(dimension);
        ((IEClientWorld) clientWorld).resetWorldRendererRef();
        clientWorldMap.remove(dimension);
        
        DimensionRenderHelper renderHelper = renderHelperMap.remove(dimension);
        if (renderHelper != null) {
            renderHelper.cleanUp();
        }
        
        Helper.log("Client Dynamically Removed Dimension " + dimension.location());
        
        if (clientWorld.getChunkSource().getLoadedChunksCount() > 0) {
            Helper.err("The chunks of that dimension was not cleared before removal");
        }
        
        if (clientWorld.getEntityCount() > 0) {
            Helper.err("The entities of that dimension was not cleared before removal");
        }
        
        client.gameRenderer.resetData();
        
        clientDimensionDynamicRemoveSignal.emit(dimension);
    }
    
    //@Nullable
    public static LevelRenderer getWorldRenderer(ResourceKey<Level> dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    
    /**
     * Get the client world and create if missing.
     * If the dimension id is invalid, it will throw an error
     */
    @NotNull
    public static ClientLevel getWorld(ResourceKey<Level> dimension) {
        Validate.notNull(dimension);
        Validate.isTrue(client.isSameThread());
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createSecondaryClientWorld(dimension);
        }
        
        ClientLevel result = clientWorldMap.get(dimension);
        Validate.notNull(result);
        return result;
    }
    
    /**
     * Get the client world and create if missing.
     * If the dimension id is invalid, it will return null
     */
    @Nullable
    public static ClientLevel getOptionalWorld(ResourceKey<Level> dimension) {
        Validate.notNull(dimension);
        Validate.isTrue(client.isSameThread());
        
        if (getServerDimensions().contains(dimension)) {
            return getWorld(dimension);
        }
        
        return null;
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
            Validate.isTrue(client.level != null, "level is null");
            Validate.isTrue(client.levelRenderer != null, "levelRenderer is null");
            
            Validate.notNull(
                client.player,
                "player is null. This may be caused by prior initialization failure. The log may provide useful information."
            );
            Validate.isTrue(client.player.level() == client.level, "The player level is not the same as client level");
            
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
        Validate.notNull(client.player);
        Validate.isTrue(client.isSameThread());
        
        Set<ResourceKey<Level>> dimIds = getServerDimensions();
        if (!dimIds.contains(dimension)) {
            throw new RuntimeException("Cannot create invalid client dimension " + dimension.location());
        }
        
        isCreatingClientWorld = true;
        
        client.getProfiler().push("create_world");
        
        int chunkLoadDistance = 3;// my own chunk manager doesn't need it
        
        LevelRenderer worldRenderer = new LevelRenderer(
            client,
            client.getEntityRenderDispatcher(),
            client.getBlockEntityRenderDispatcher(),
            client.renderBuffers()
        );
        
        ClientLevel newWorld;
        try {
            ClientPacketListener mainNetHandler = client.player.connection;
            Map<String, MapItemSavedData> mapData = ((IEClientLevel_Accessor) client.level).ip_getMapData();
            
            ResourceKey<DimensionType> dimensionTypeKey =
                DimensionTypeSync.getDimensionTypeKey(dimension);
            ClientLevel.ClientLevelData currentProperty =
                (ClientLevel.ClientLevelData) ((IEWorld) client.level).myGetProperties();
            RegistryAccess registryManager = mainNetHandler.registryAccess();
            int simulationDistance = client.level.getServerSimulationDistance();
            
            Holder<DimensionType> dimensionType = registryManager
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(dimensionTypeKey);
            
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
            
            // all worlds share the same map data map
            ((IEClientLevel_Accessor) newWorld).ip_setMapData(mapData);
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Client World " + dimension + " " + clientWorldMap.keySet(),
                e
            );
        }
        
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
    
    public static Set<ResourceKey<Level>> getServerDimensions() {
        return client.player.connection.levels();
    }
    
    public static Collection<ClientLevel> getClientWorlds() {
        Validate.isTrue(isInitialized);
        
        return clientWorldMap.values();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    public static void _onWorldRendererReloaded() {
        Validate.isTrue(client.isSameThread());
        if (client.level != null) {
            Helper.log("WorldRenderer reloaded " + client.level.dimension().location());
        }
        
        if (isReloadingOtherWorldRenderers) {
            return;
        }
        if (PortalRendering.isRendering()) {
            return;
        }
        if (ClientWorldLoader.getIsCreatingClientWorld()) {
            return;
        }
        
        isReloadingOtherWorldRenderers = true;
        
        List<ResourceKey<Level>> toReload = worldRendererMap.keySet().stream()
            .filter(d -> d != client.level.dimension()).collect(Collectors.toList());
        
        for (ResourceKey<Level> dim : toReload) {
            ClientLevel world = clientWorldMap.get(dim);
            Validate.notNull(world);
            withSwitchedWorld(
                world,
                () -> {
                    // cannot be replaced into method reference
                    // because levelRenderer field is actually mutable
                    client.levelRenderer.allChanged();
                }
            );
        }
        
        isReloadingOtherWorldRenderers = false;
    }
    
    /**
     * It will not switch the dimension of client player
     */
    public static <T> T withSwitchedWorld(ClientLevel newWorld, Supplier<T> supplier) {
        Validate.isTrue(client.isSameThread());
        Validate.isTrue(client.player != null);
        
        ClientPacketListener networkHandler = client.getConnection();
        
        ClientLevel originalWorld = client.level;
        LevelRenderer originalWorldRenderer = client.levelRenderer;
        ClientLevel originalNetHandlerWorld = networkHandler.getLevel();
        boolean originalIsWorldSwitched = isWorldSwitched;
        
        LevelRenderer newWorldRenderer = getWorldRenderer(newWorld.dimension());
        
        Validate.notNull(newWorldRenderer);
        
        client.level = newWorld;
        ((IEParticleManager) client.particleEngine).ip_setWorld(newWorld);
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        ((IEClientPlayNetworkHandler) networkHandler).ip_setWorld(newWorld);
        isWorldSwitched = true;
        
        try {
            
            return supplier.get();
        }
        finally {
            if (client.level != newWorld) {
                Helper.err("Respawn packet should not be redirected");
                originalWorld = client.level;
                originalWorldRenderer = client.levelRenderer;
            }
            
            client.level = originalWorld;
            ((IEMinecraftClient) client).setWorldRenderer(originalWorldRenderer);
            ((IEParticleManager) client.particleEngine).ip_setWorld(originalWorld);
            ((IEClientPlayNetworkHandler) networkHandler).ip_setWorld(originalNetHandlerWorld);
            isWorldSwitched = originalIsWorldSwitched;
        }
    }
    
    public static void withSwitchedWorld(ClientLevel newWorld, Runnable runnable) {
        withSwitchedWorld(newWorld, () -> {
            runnable.run();
            return null;
        });
    }
    
    public static void withSwitchedWorldFailSoft(ResourceKey<Level> dim, Runnable runnable) {
        ClientLevel world = getOptionalWorld(dim);
        
        if (world == null) {
            Helper.err(
                "Ignoring redirected task of invalid dimension %s"
                    .formatted(dim.location())
            );
            return;
        }
        
        withSwitchedWorld(world, runnable);
    }
    
    public static boolean getIsWorldSwitched() {
        return isWorldSwitched;
    }
    
    public static class RemoteCallables {
        public static void checkBiomeRegistry(
            Map<String, Integer> idMap
        ) {
            RegistryAccess registryAccess = Minecraft.getInstance().player.connection.registryAccess();
            Registry<Biome> biomes = registryAccess.registryOrThrow(Registries.BIOME);
            
            for (Map.Entry<String, Integer> entry : idMap.entrySet()) {
                ResourceLocation id = new ResourceLocation(entry.getKey());
                int expectedId = entry.getValue();
                
                if (biomes.getId(biomes.get(id)) != expectedId) {
                    LOGGER.error("Biome id mismatch: " + id + " " + expectedId);
                }
            }
            
            if (idMap.size() != biomes.keySet().size()) {
                LOGGER.error("Biome id mismatch: size not equal");
            }
            
            LOGGER.info("Biome id check finished");
        }
    }
}
