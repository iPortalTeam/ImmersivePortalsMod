package qouteall.imm_ptl.core;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
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
import qouteall.dimlib.api.DimensionAPI;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.mixin.client.accessor.IEClientLevelData;
import qouteall.imm_ptl.core.mixin.client.accessor.IEClientLevel_Accessor;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.DimensionRenderHelper;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.CountDownInt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("resource")
@Environment(EnvType.CLIENT)
public class ClientWorldLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientWorldLoader.class);
    
    private static final CountDownInt LOG_LIMIT = new CountDownInt(20);
    
    public static final Event<Consumer<ResourceKey<Level>>> CLIENT_DIMENSION_DYNAMIC_REMOVE_EVENT =
        Helper.createConsumerEvent();
    public static final Event<Consumer<ClientLevel>> CLIENT_WORLD_LOAD_EVENT =
        Helper.createConsumerEvent();
    
    private static final Map<ResourceKey<Level>, ClientLevel> CLIENT_WORLD_MAP =
        new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceKey<Level>, LevelRenderer> WORLD_RENDERER_MAP =
        new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceKey<Level>, DimensionRenderHelper> RENDER_HELPER_MAP =
        new Object2ObjectOpenHashMap<>();
    
    public static @Nullable Map<ResourceKey<Level>, ResourceKey<DimensionType>> dimIdToDimTypeId;
    
    private static final Minecraft client = Minecraft.getInstance();
    
    private static boolean isInitialized = false;
    
    private static boolean isCreatingClientWorld = false;
    
    public static boolean isClientRemoteTicking = false;
    
    private static boolean isWorldSwitched = false;
    
    public static void init() {
        DimensionAPI.CLIENT_DIMENSION_UPDATE_EVENT.register((serverDimensions) -> {
            if (getIsInitialized()) {
                List<ResourceKey<Level>> dimensionsToRemove =
                    CLIENT_WORLD_MAP.keySet().stream()
                        .filter(dim -> !serverDimensions.contains(dim)).toList();
                
                for (ResourceKey<Level> dim : dimensionsToRemove) {
                    disposeDimensionDynamically(dim);
                }
                
            }
        });
        
        IPCGlobal.CLIENT_EXIT_EVENT.register(() -> {
            dimIdToDimTypeId = null;
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
            CLIENT_WORLD_MAP.values().forEach(world -> {
                if (client.level != world) {
                    tickRemoteWorld(world);
                }
            });
            WORLD_RENDERER_MAP.values().forEach(worldRenderer -> {
                if (worldRenderer != client.levelRenderer) {
                    worldRenderer.tick();
                }
            });
            isClientRemoteTicking = false;
        }
        
        boolean lightmapTextureConflict = false;
        for (DimensionRenderHelper helper : RENDER_HELPER_MAP.values()) {
            helper.tick();
            if (helper.world != client.level) {
                if (helper.lightmapTexture == client.gameRenderer.lightTexture()) {
                    assert client.level != null;
                    LOGGER.info(
                        "Lightmap Texture Conflict {} {}",
                        helper.world.dimension().location(),
                        client.level.dimension().location()
                    );
                    lightmapTextureConflict = true;
                }
            }
        }
        if (lightmapTextureConflict) {
            disposeRenderHelpers();
            LOGGER.info("Refreshed Lightmaps");
        }
        
    }
    
    public static void disposeRenderHelpers() {
        RENDER_HELPER_MAP.values().forEach(DimensionRenderHelper::cleanUp);
        RENDER_HELPER_MAP.clear();
    }
    
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
                if (LOG_LIMIT.tryDecrement()) {
                    LOGGER.error("", e);
                }
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
            assert client.player != null;
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
    
    public static void cleanUp() {
        WORLD_RENDERER_MAP.values().forEach(
            ClientWorldLoader::disposeWorldRenderer
        );
        
        for (ClientLevel clientWorld : CLIENT_WORLD_MAP.values()) {
            ((IEClientWorld) clientWorld).ip_resetWorldRendererRef();
        }
        
        CLIENT_WORLD_MAP.clear();
        WORLD_RENDERER_MAP.clear();
        
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
        Validate.notNull(client.player, "player is null");
        Validate.notNull(client.level, "level is null");
        Validate.isTrue(
            client.level.dimension() != dimension,
            "Cannot dispose current dimension"
        );
        Validate.isTrue(
            client.player.level().dimension() != dimension,
            "Cannot dispose current dimension"
        );
        Validate.isTrue(client.isSameThread(), "not on client thread");
        
        LevelRenderer worldRenderer = WORLD_RENDERER_MAP.get(dimension);
        disposeWorldRenderer(worldRenderer);
        WORLD_RENDERER_MAP.remove(dimension);
        
        Validate.isTrue(client.levelRenderer != worldRenderer);
        
        ClientLevel clientWorld = CLIENT_WORLD_MAP.get(dimension);
        ((IEClientWorld) clientWorld).ip_resetWorldRendererRef();
        CLIENT_WORLD_MAP.remove(dimension);
        
        DimensionRenderHelper renderHelper = RENDER_HELPER_MAP.remove(dimension);
        if (renderHelper != null) {
            renderHelper.cleanUp();
        }
        
        LOGGER.info("Client Dynamically Removed Dimension {}", dimension.location());
        
        if (clientWorld.getChunkSource().getLoadedChunksCount() > 0) {
            LOGGER.error("The chunks of that dimension was not cleared before removal");
        }
        
        if (clientWorld.getEntityCount() > 0) {
            LOGGER.error("The entities of that dimension was not cleared before removal");
        }
        
        client.gameRenderer.resetData();
        
        CLIENT_DIMENSION_DYNAMIC_REMOVE_EVENT.invoker().accept(dimension);
    }
    
    @NotNull
    public static LevelRenderer getWorldRenderer(ResourceKey<Level> dimension) {
        initializeIfNeeded();
        
        LevelRenderer result = WORLD_RENDERER_MAP.get(dimension);
        
        if (result == null) {
            LOGGER.warn(
                "Acquiring LevelRenderer before acquiring Level. Something is probably wrong. {}",
                dimension.location(), new Throwable()
            );
            
            // the world renderer is created along with the world
            // so create the world now
            getWorld(dimension);
            
            result = WORLD_RENDERER_MAP.get(dimension);
            
            if (result == null) {
                throw new RuntimeException("Unable to get LevelRenderer of " + dimension.location());
            }
        }
        
        return result;
    }
    
    
    /**
     * Get the client world and create if missing.
     * If the dimension id is invalid, it will throw an error
     */
    @NotNull
    public static ClientLevel getWorld(ResourceKey<Level> dimension) {
        Validate.notNull(dimension, "dimension is null");
        Validate.isTrue(client.isSameThread());
        
        initializeIfNeeded();
        
        if (!CLIENT_WORLD_MAP.containsKey(dimension)) {
            return createSecondaryClientWorld(dimension);
        }
        
        ClientLevel result = CLIENT_WORLD_MAP.get(dimension);
        Validate.notNull(result, "null value in world map");
        return result;
    }
    
    /**
     * Get the client world and create if missing.
     * If the dimension id is invalid, it will return null
     */
    @Nullable
    public static ClientLevel getOptionalWorld(ResourceKey<Level> dimension) {
        Validate.notNull(dimension, "dimension is null");
        Validate.isTrue(client.isSameThread(), "not on client thread");
        
        if (getServerDimensions().contains(dimension)) {
            return getWorld(dimension);
        }
        
        return null;
    }
    
    public static DimensionRenderHelper getDimensionRenderHelper(ResourceKey<Level> dimension) {
        initializeIfNeeded();
        
        DimensionRenderHelper result = RENDER_HELPER_MAP.computeIfAbsent(
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
    
    @SuppressWarnings("ConstantValue")
    public static void initializeIfNeeded() {
        if (!isInitialized) {
            Validate.isTrue(
                client.level != null, "level is null"
            );
            // note: client.levelRenderer is not necessarily not null due to mixin
            Validate.isTrue(
                client.levelRenderer != null, "levelRenderer is null"
            );
            
            Validate.notNull(
                client.player,
                "player is null. This may be caused by prior initialization failure. The log may provide useful information."
            );
            Validate.isTrue(
                client.player.level() == client.level,
                "The player level is not the same as client level"
            );
            
            ResourceKey<Level> playerDimension = client.level.dimension();
            CLIENT_WORLD_MAP.put(playerDimension, client.level);
            WORLD_RENDERER_MAP.put(playerDimension, client.levelRenderer);
            RENDER_HELPER_MAP.put(
                client.level.dimension(),
                new DimensionRenderHelper(client.level)
            );
            
            isInitialized = true;
        }
    }
    
    @SuppressWarnings("DataFlowIssue")
    private static ClientLevel createSecondaryClientWorld(ResourceKey<Level> dimension) {
        Validate.notNull(client.player, "player is null");
        Validate.isTrue(client.isSameThread(), "not on client thread");
        
        Set<ResourceKey<Level>> dimIds = getServerDimensions();
        if (!dimIds.contains(dimension)) {
            throw new RuntimeException("Cannot create invalid client dimension " + dimension.location());
        }
        
        isCreatingClientWorld = true;
        
        client.getProfiler().push("create_world");
        
        int chunkLoadDistance = 3; // my own chunk manager doesn't need it
        
        LevelRenderer worldRenderer = new LevelRenderer(
            client,
            client.getEntityRenderDispatcher(),
            client.getBlockEntityRenderDispatcher(),
            client.renderBuffers()
        );
        
        ClientLevel newWorld;
        try {
            ClientPacketListener mainNetHandler = client.player.connection;
            assert client.level != null;
            Map<String, MapItemSavedData> mapData = ((IEClientLevel_Accessor) client.level).ip_getMapData();
            
            Validate.notNull(
                dimIdToDimTypeId, "dimension type mapping is missing"
            );
            ResourceKey<DimensionType> dimensionTypeKey = dimIdToDimTypeId.get(dimension);
            
            if (dimensionTypeKey == null) {
                throw new IllegalStateException(
                    "Cannot find dimension type for %s in %s"
                        .formatted(dimension.location(), dimIdToDimTypeId)
                );
            }
            
            ClientLevel.ClientLevelData currentProperty =
                (ClientLevel.ClientLevelData) ((IEWorld) client.level).ip_getLevelData();
            RegistryAccess registryManager = mainNetHandler.registryAccess();
            int simulationDistance = client.level.getServerSimulationDistance();
            
            Holder<DimensionType> dimensionType = registryManager
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(dimensionTypeKey);
            
            ClientLevel.ClientLevelData properties = new ClientLevel.ClientLevelData(
                currentProperty.getDifficulty(),
                currentProperty.isHardcore(),
                ((IEClientLevelData) currentProperty).ip_getIsFlat()
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
            
            worldRenderer.setLevel(newWorld);
            
            worldRenderer.onResourceManagerReload(client.getResourceManager());
            
            CLIENT_WORLD_MAP.put(dimension, newWorld);
            WORLD_RENDERER_MAP.put(dimension, worldRenderer);
            
            LOGGER.info("Client World Created {}", dimension.location());
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Client World " + dimension.location() + " " + CLIENT_WORLD_MAP.keySet(),
                e
            );
        }
        finally {
            isCreatingClientWorld = false;
            client.getProfiler().pop();
        }
        
        CLIENT_WORLD_LOAD_EVENT.invoker().accept(newWorld);
        
        return newWorld;
    }
    
    public static Set<ResourceKey<Level>> getServerDimensions() {
        assert client.player != null;
        return client.player.connection.levels();
    }
    
    public static Collection<ClientLevel> getClientWorlds() {
        Validate.isTrue(isInitialized);
        
        return CLIENT_WORLD_MAP.values();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    @SuppressWarnings("Convert2MethodRef")
    public static void _onWorldRendererReloaded() {
        Validate.isTrue(client.isSameThread());
        if (client.level != null) {
            LOGGER.info("WorldRenderer reloaded {}", client.level.dimension().location());
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
        
        List<ResourceKey<Level>> toReload = WORLD_RENDERER_MAP.keySet().stream()
            .filter(d -> d != client.level.dimension()).toList();
        
        for (ResourceKey<Level> dim : toReload) {
            ClientLevel world = CLIENT_WORLD_MAP.get(dim);
            Validate.notNull(world, "missing client world %s", dim.location());
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
    @SuppressWarnings({"ReassignedVariable", "DataFlowIssue"})
    public static <T> T withSwitchedWorld(ClientLevel newWorld, Supplier<T> supplier) {
        Validate.isTrue(client.isSameThread(), "not on client thread");
        Validate.isTrue(client.player != null, "player is null");
        
        ClientPacketListener networkHandler = client.getConnection();
        assert networkHandler != null;
        
        ClientLevel originalWorld = client.level;
        LevelRenderer originalWorldRenderer = client.levelRenderer;
        ClientLevel originalNetHandlerWorld = networkHandler.getLevel();
        boolean originalIsWorldSwitched = isWorldSwitched;
        
        LevelRenderer newWorldRenderer = getWorldRenderer(newWorld.dimension());
        
        Validate.notNull(newWorldRenderer, "new world renderer is null");
        
        client.level = newWorld;
        ((IEParticleManager) client.particleEngine).ip_setWorld(newWorld);
        ((IEMinecraftClient) client).ip_setWorldRenderer(newWorldRenderer);
        ((IEClientPlayNetworkHandler) networkHandler).ip_setWorld(newWorld);
        isWorldSwitched = true;
        
        try {
            return supplier.get();
        }
        finally {
            if (client.level != newWorld) {
                LOGGER.error("Respawn packet should not be redirected");
                originalWorld = client.level;
                originalWorldRenderer = client.levelRenderer;
                // client.levelRenderer is not final by mixin.
            }
            
            client.level = originalWorld;
            ((IEMinecraftClient) client).ip_setWorldRenderer(originalWorldRenderer);
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
            LOGGER.error(
                "Ignoring redirected task of invalid dimension {}", dim.location(), new Throwable()
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
            LocalPlayer player = Minecraft.getInstance().player;
            assert player != null;
            RegistryAccess registryAccess = player.connection.registryAccess();
            Registry<Biome> biomes = registryAccess.registryOrThrow(Registries.BIOME);
            
            for (Map.Entry<String, Integer> entry : idMap.entrySet()) {
                ResourceLocation id = new ResourceLocation(entry.getKey());
                int expectedId = entry.getValue();
                
                if (biomes.getId(biomes.get(id)) != expectedId) {
                    LOGGER.error("Biome id mismatch: {} {}", id, expectedId);
                }
            }
            
            if (idMap.size() != biomes.keySet().size()) {
                LOGGER.error("Biome id mismatch: size not equal");
            }
            
            LOGGER.info("Biome id check finished");
        }
    }
}
