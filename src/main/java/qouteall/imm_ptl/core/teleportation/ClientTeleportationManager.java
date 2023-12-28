package qouteall.imm_ptl.core.teleportation;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.imm_ptl.core.collision.PortalCollisionHandler;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.ducks.IEAbstractClientPlayer;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.network.ImmPtlNetworking;
import qouteall.imm_ptl.core.network.PacketRedirectionClient;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.animation.ClientPortalAnimationManagement;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.FogRendererContext;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Vec2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class ClientTeleportationManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public static long tickTimeForTeleportation = 0;
    private static long lastTeleportGameTime = 0;
    private static long teleportTickTimeLimit = 0;
    
    private static Vec3 lastPlayerEyePos = null;
    private static long lastRecordStableTickTime = 0;
    private static float lastRecordStablePartialTicks = 0;
    
    // for debug
    public static boolean isTeleportingTick = false;
    public static boolean isTeleportingFrame = false;
    public static boolean isTicking = false;
    
    private static final int teleportLimitPerFrame = 3;
    
    private static long teleportationCounter = 0;
    
    public static void init() {
        IPGlobal.POST_CLIENT_TICK_EVENT.register(
            ClientTeleportationManager::tick
        );
        
        IPCGlobal.CLIENT_CLEANUP_EVENT.register(() -> {
            lastPlayerEyePos = null;
//            disableTeleportFor(2);
        });
    }
    
    private static void tick() {
        tickTimeForTeleportation++;
        changePlayerMotionIfCollidingWithPortal();
        
        isTeleportingTick = false;
    }
    
    public static void acceptSynchronizationDataFromServer(
        ResourceKey<Level> dimension,
        Vec3 pos,
        boolean forceAccept
    ) {
        if (!forceAccept) {
            if (isTeleportingFrequently()) {
                return;
            }
            // newly teleported by vanilla means
            if (client.player.tickCount < 200) {
                return;
            }
        }
        if (client.player.level().dimension() != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
    }
    
    public static void manageTeleportation(boolean isTicking_) {
        if (IPGlobal.disableTeleportation) {
            return;
        }
        
        isTicking = isTicking_;
        
        teleportationCounter++;
        
        isTeleportingFrame = false;
        
        if (client.level == null || client.player == null) {
            lastPlayerEyePos = null;
            return;
        }
        
        // not initialized
        if (client.player.xo == 0 && client.player.yo == 0 && client.player.zo == 0) {
            return;
        }
        
        client.getProfiler().push("ip_teleport");
        
        ClientPortalAnimationManagement.foreachCustomAnimatedPortals(
            portal -> {
                // update teleportation-related data
                PortalExtension.forClusterPortals(
                    portal, p -> p.animation.updateClientState(p, teleportationCounter)
                );
            }
        );

//        ClientPortalAnimationManagement.debugCheck();
        
        // the real partial ticks (not from stable timer)
        float realPartialTicks = RenderStates.getPartialTick();
        
        TeleportationUtil.Teleportation lastTeleportation = null;
        ResourceKey<Level> originalDim = client.player.level().dimension();
        
        if (lastPlayerEyePos != null) {
            for (int i = 0; i <= teleportLimitPerFrame; i++) {
                TeleportationUtil.Teleportation teleportation = tryTeleport(realPartialTicks);
                if (teleportation == null) {
                    break;
                }
                else {
                    lastTeleportation = teleportation;
                    if (i != 0) {
                        LOGGER.info("The client player made a combo-teleport");
                        if (i == teleportLimitPerFrame) {
                            // we should reject combo teleportation of too many layers
                            // if not, the player can escape a fractal scale box from the portal that overlaps its two sides
                            LOGGER.info("Combo teleport out of limit. Reject teleportation!");
                            Vec3 oldPos =
                                lastPlayerEyePos.subtract(McHelper.getEyeOffset(client.player));
                            forceTeleportPlayer(
                                originalDim,
                                oldPos.add(teleportation.worldSurfaceNormal().scale(-0.001))
                            );
                            client.player.setDeltaMovement(
                                teleportation.worldSurfaceNormal().scale(-0.1)
                            );
                            lastTeleportation = null;
                            break;
                        }
                    }
                }
            }
        }
        
        if (lastTeleportation != null) {
            if (PortalExtension.get(lastTeleportation.portal()).adjustPositionAfterTeleport) {
                adjustPlayerPosition(client.player);
            }
        }
        
        lastPlayerEyePos = getPlayerEyePos(realPartialTicks);
        lastRecordStableTickTime = StableClientTimer.getStableTickTime();
        lastRecordStablePartialTicks = StableClientTimer.getStablePartialTicks();
        
        client.getProfiler().pop();
    }
    
    private static record TeleportationRec(
        Portal portal, Vec2d portalLocalXY, Vec3 collisionPos
    ) {}
    
    // return null if failed
    @Nullable
    private static TeleportationUtil.Teleportation tryTeleport(float partialTicks) {
        LocalPlayer player = client.player;
        assert player != null;
        
        Vec3 thisFrameEyePos = getPlayerEyePos(partialTicks);
        
        if (lastPlayerEyePos.distanceToSqr(thisFrameEyePos) > 1600) {
            // when the player is moving too fast, don't do teleportation
            return null;
        }
        
        assert client.level != null;
        long currentGameTime = client.level.getGameTime();
        
        Vec3 lastTickEyePos = McHelper.getLastTickEyePos(player);
        Vec3 thisTickEyePos = McHelper.getEyePos(player);
        
        ArrayList<TeleportationUtil.Teleportation> teleportationCandidates = new ArrayList<>();
        IPMcHelper.traverseNearbyPortals(
            player.level(),
            thisFrameEyePos,
            IPGlobal.maxNormalPortalRadius,
            portal -> {
                if (!portal.canTeleportEntity(player)) {
                    return;
                }
                
                // Separately handle dynamic teleportation and static teleportation.
                // Although the dynamic teleportation code can handle static teleportation.
                // I want the dynamic teleportation bugs to not affect static teleportation.
                if (portal.animation.clientLastFramePortalStateCounter == teleportationCounter - 1
                    && portal.animation.clientLastFramePortalState != null
                    && portal.animation.lastTickAnimatedState != null
                    && portal.animation.thisTickAnimatedState != null
                ) {
                    // the portal is running a real animation
                    assert portal.animation.clientCurrentFramePortalState != null;
                    
                    TeleportationUtil.Teleportation teleportation =
                        TeleportationUtil.checkDynamicTeleportation(
                            portal,
                            portal.animation.clientLastFramePortalState,
                            portal.animation.clientCurrentFramePortalState,
                            lastPlayerEyePos,
                            thisFrameEyePos,
                            portal.animation.lastTickAnimatedState,
                            portal.animation.thisTickAnimatedState,
                            lastTickEyePos,
                            thisTickEyePos,
                            partialTicks
                        );
                    
                    if (teleportation != null) {
                        teleportationCandidates.add(teleportation);
                    }
                }
                else {
                    // the portal is static
                    TeleportationUtil.Teleportation teleportation =
                        TeleportationUtil.checkStaticTeleportation(
                            portal,
                            lastPlayerEyePos, thisFrameEyePos,
                            lastTickEyePos, thisTickEyePos
                        );
                    if (teleportation != null) {
                        teleportationCandidates.add(teleportation);
                    }
                }
            }
        );
        
        TeleportationUtil.Teleportation teleportation = teleportationCandidates
            .stream()
            .min(Comparator.comparingDouble(
                p -> p.worldCollisionPoint().distanceToSqr(lastPlayerEyePos)
            ))
            .orElse(null);
        
        
        if (teleportation != null) {
            Portal portal = teleportation.portal();
            Vec3 collidingPos = teleportation.worldCollisionPoint();
            
            client.getProfiler().push("portal_teleport");
            teleportPlayer(teleportation, partialTicks);
            client.getProfiler().pop();
            
            boolean allowOverlappedTeleport = portal.respectParallelOrientedPortal();
            
            // avoid teleporting through parallel portal due to floating point inaccuracy
            double adjustment = allowOverlappedTeleport ? -0.001 : 0.001;
            
            Vec3 newDelta = teleportation.newThisTickEyePos()
                .subtract(teleportation.newLastTickEyePos());
            
            lastPlayerEyePos = teleportation.teleportationCheckpoint()
                .add(newDelta.scale(adjustment));
            
            return teleportation;
        }
        else {
            return null;
        }
    }
    
    public static Vec3 getPlayerEyePos(float partialTick) {
        return client.player.getEyePosition(partialTick);
    }
    
    private static void teleportPlayer(
        TeleportationUtil.Teleportation teleportation, float partialTicks
    ) {
        Portal portal = teleportation.portal();
        
        if (tickTimeForTeleportation <= teleportTickTimeLimit) {
            Helper.log("Client player teleportation rejected");
            return;
        }
        
        lastTeleportGameTime = tickTimeForTeleportation;
        
        LocalPlayer player = client.player;
        Validate.isTrue(player != null);
        
        ResourceKey<Level> toDimension = portal.getDestDim();
        float tickDelta = RenderStates.getPartialTick();
        
        Entity vehicle = player.getVehicle();
        Vec3 oldVehiclePos = vehicle != null ? vehicle.position() : null;
        
        Vec3 thisTickEyePos = McHelper.getEyePos(player);
        Vec3 lastTickEyePos = McHelper.getLastTickEyePos(player);
        
        Vec3 newThisTickEyePos = teleportation.newThisTickEyePos();
        Vec3 newLastTickEyePos = teleportation.newLastTickEyePos();
        
        ClientLevel fromWorld = client.level;
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        
        if (fromDimension != toDimension) {
            ClientLevel toWorld = ClientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, newThisTickEyePos);
        }
        
        McHelper.setEyePos(player, newThisTickEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
        
        Vec3 oldRealVelocity = McHelper.getWorldVelocity(player);
        TransformationManager.managePlayerRotationAndChangeGravity(portal);
        McHelper.setWorldVelocity(player, oldRealVelocity); // reset velocity change
        
        TeleportationUtil.PortalPointVelocity portalPointVelocity = teleportation.portalPointVelocity();
        TeleportationUtil.transformEntityVelocity(portal, player, portalPointVelocity, thisTickEyePos);
        
        if (vehicle != null) {
            TeleportationUtil.transformEntityVelocity(portal, vehicle, portalPointVelocity, oldVehiclePos);
        }
        
        PehkuiInterface.invoker.onClientPlayerTeleported(portal);
        
        player.connection.send(ClientPlayNetworking.createC2SPacket(
            new ImmPtlNetworking.TeleportPacket(
                PortalAPI.clientDimKeyToInt(fromDimension),
                thisTickEyePos,
                portal.getUUID()
            )
        ));
        
        PortalCollisionHandler.updateCollidingPortalAfterTeleportation(
            player, newThisTickEyePos, newLastTickEyePos, RenderStates.getPartialTick()
        );
        
        McHelper.adjustVehicle(player);
        
        //because the teleportation may happen before rendering
        //but after pre render info being updated
        RenderStates.updatePreRenderInfo(tickDelta);
        
        if (teleportation.isDynamic()) {
            LOGGER.info(
                """
                    Client Teleported Dynamically
                    portal: {}
                    tickTime: {}
                    during ticking: {}
                    counter: {}
                    eye pos (by frame): {} -> {}
                    partial ticks: {}
                    new immediate eye pos: {}
                    portal origin/normal: {} {}
                    portal dest/content dir: {} {}""",
                portal, tickTimeForTeleportation, isTicking, teleportationCounter,
                teleportation.lastWorldEyePos(), teleportation.currentWorldEyePos(), partialTicks,
                teleportation.newLastTickEyePos().lerp(teleportation.newThisTickEyePos(), tickDelta),
                portal.getOriginPos(), portal.getNormal(),
                portal.getDestPos(), portal.getContentDirection()
            );
        }
        else {
            LOGGER.info(
                """
                    Client Teleported Statically
                    portal: {}
                    eye pos: {} -> {}""",
                portal, teleportation.lastWorldEyePos(), teleportation.currentWorldEyePos()
            );
        }
        
        isTeleportingTick = true;
        isTeleportingFrame = true;
        
        MyGameRenderer.vanillaTerrainSetupOverride = 1;
    }
    
    
    public static boolean isTeleportingFrequently() {
        return (tickTimeForTeleportation - lastTeleportGameTime <= 100) ||
            (tickTimeForTeleportation <= teleportTickTimeLimit);
    }
    
    public static void forceTeleportPlayer(ResourceKey<Level> toDimension, Vec3 destination) {
        LOGGER.info("client player force teleported {} {}", toDimension.location(), destination);
        
        ClientLevel fromWorld = client.level;
        assert fromWorld != null;
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        LocalPlayer player = client.player;
        assert player != null;
        if (fromDimension != toDimension) {
            ClientLevel toWorld = ClientWorldLoader.getWorld(toDimension);
            Vec3 eyeOffset = McHelper.getEyeOffset(player);
            changePlayerDimension(player, fromWorld, toWorld, destination.add(eyeOffset));
        }
        
        player.setPos(destination.x, destination.y, destination.z);
        McHelper.adjustVehicle(player);
        
        lastPlayerEyePos = null;
        
        RenderStates.updatePreRenderInfo(RenderStates.getPartialTick());
        MyGameRenderer.vanillaTerrainSetupOverride = 1;
    }
    
    /**
     * {@link ClientPacketListener#handleRespawn(ClientboundRespawnPacket)}
     */
    public static void changePlayerDimension(
        LocalPlayer player, ClientLevel fromWorld, ClientLevel toWorld, Vec3 newEyePos
    ) {
        Validate.isTrue(!WorldRenderInfo.isRendering());
        Validate.isTrue(!FrontClipping.isClippingEnabled);
        Validate.isTrue(!PacketRedirectionClient.getIsProcessingRedirectedMessage());
        
        Entity vehicle = player.getVehicle();
        player.unRide();
        
        ResourceKey<Level> toDimension = toWorld.dimension();
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        
        ((IEClientPlayNetworkHandler) client.getConnection()).ip_setWorld(toWorld);
        
        fromWorld.removeEntity(player.getId(), Entity.RemovalReason.CHANGED_DIMENSION);
        
        ((IEEntity) player).ip_setWorld(toWorld);
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        ((IEEntity) player).ip_unsetRemoved();
        
        toWorld.addEntity(player);
        ((IEAbstractClientPlayer) player).ip_setClientLevel(toWorld);
        
        IEGameRenderer gameRenderer = (IEGameRenderer) Minecraft.getInstance().gameRenderer;
        gameRenderer.ip_setLightmapTextureManager(ClientWorldLoader
            .getDimensionRenderHelper(toDimension).lightmapTexture);
        
        client.level = toWorld;
        ((IEMinecraftClient) client).ip_setWorldRenderer(
            ClientWorldLoader.getWorldRenderer(toDimension)
        );
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (client.particleEngine != null) {
            // avoid clearing all particles
            ((IEParticleManager) client.particleEngine).ip_setWorld(toWorld);
        }
        
        client.getBlockEntityRenderDispatcher().setLevel(toWorld);
        
        if (vehicle != null) {
            Vec3 offset = McHelper.getVehicleOffsetFromPassenger(vehicle, player);
            Vec3 vehiclePos = player.position().add(offset);
            moveClientEntityAcrossDimension(
                vehicle, toWorld,
                vehiclePos
            );
            McHelper.setPosAndLastTickPos(
                vehicle,
                player.position().add(offset),
                McHelper.lastTickPosOf(player).add(offset)
            );
            player.startRiding(vehicle, true);
        }
        
        Helper.log(String.format(
            "Client Changed Dimension from %s to %s time: %s age: %s",
            fromDimension.location(),
            toDimension.location(),
            tickTimeForTeleportation,
            player.tickCount
        ));
        
        FogRendererContext.onPlayerTeleport(fromDimension, toDimension);
        
        O_O.onPlayerChangeDimensionClient(fromDimension, toDimension);
    }
    
    private static void changePlayerMotionIfCollidingWithPortal() {
        LocalPlayer player = client.player;
        
        Portal portal = ((IEEntity) player).ip_getCollidingPortal();
        
        if (portal != null) {
            if (PortalExtension.get(portal).motionAffinity > 0) {
                changeMotion(player, portal);
            }
            else if (PortalExtension.get(portal).motionAffinity < 0) {
                if (player.getDeltaMovement().length() > 0.7) {
                    changeMotion(player, portal);
                }
            }
        }
    }
    
    private static void changeMotion(Entity player, Portal portal) {
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.scale(1 + PortalExtension.get(portal).motionAffinity));
    }
    
    //foot pos, not eye pos
    public static void moveClientEntityAcrossDimension(
        Entity entity,
        ClientLevel newWorld,
        Vec3 newPos
    ) {
        ClientLevel oldWorld = (ClientLevel) entity.level();
        oldWorld.removeEntity(entity.getId(), Entity.RemovalReason.CHANGED_DIMENSION);
        ((IEEntity) entity).ip_setWorld(newWorld);
        entity.setPos(newPos.x, newPos.y, newPos.z);
        ((IEEntity) entity).ip_unsetRemoved();
        newWorld.addEntity(entity);
        Validate.isTrue(!entity.isRemoved());
    }
    
    public static void disableTeleportFor(int ticks) {
        teleportTickTimeLimit = tickTimeForTeleportation + ticks;
    }
    
    private static void adjustPlayerPosition(LocalPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        
        if (player.getVehicle() != null) {
            return;
        }
        
        AABB playerBoundingBox = player.getBoundingBox();
        PortalCollisionHandler portalCollisionHandler = ((IEEntity) player).ip_getPortalCollisionHandler();
        List<Portal> collidingPortals = portalCollisionHandler == null ?
            Collections.emptyList() : portalCollisionHandler.getCollidingPortals();
        
        Direction gravityDir = GravityChangerInterface.invoker.getGravityDirection(player);
        Direction levitationDir = gravityDir.getOpposite();
        Vec3 eyeOffset = GravityChangerInterface.invoker.getEyeOffset(player);
        
        AABB bottomHalfBox = playerBoundingBox.contract(eyeOffset.x / 2, eyeOffset.y / 2, eyeOffset.z / 2);
        Function<VoxelShape, VoxelShape> shapeFilter = c -> {
            VoxelShape curr = c;
            for (Portal collidingPortal : collidingPortals) {
                Plane outerClipping = collidingPortal.getPortalShape()
                    .getOuterClipping(collidingPortal.getThisSideState());
                
                if (outerClipping != null) {
                    curr = CollisionHelper.clipVoxelShape(
                        curr, outerClipping.pos(), outerClipping.normal()
                    );
                    if (curr == null) {
                        return null;
                    }
                }
            }
            
            return curr;
        };
        
        AABB collisionUnion = CollisionHelper.getTotalBlockCollisionBox(
            player, bottomHalfBox, shapeFilter
        );
        
        if (collisionUnion == null) {
            return;
        }
        
        Vec3 anchor = player.position();
        AABB collisionUnionLocal = Helper.transformBox(
            collisionUnion, v -> GravityChangerInterface.invoker.transformWorldToPlayer(
                gravityDir, v.subtract(anchor)
            )
        );
        
        AABB playerBoxLocal = Helper.transformBox(
            playerBoundingBox, v -> GravityChangerInterface.invoker.transformWorldToPlayer(
                gravityDir, v.subtract(anchor)
            )
        );
        
        double targetLocalY = collisionUnionLocal.maxY + 0.01;
        double originalLocalY = playerBoxLocal.minY;
        double delta = targetLocalY - originalLocalY;
        
        if (delta <= 0) {
            return;
        }
        
        Vec3 levitationVec = Vec3.atLowerCornerOf(levitationDir.getNormal());
        
        Vec3 offset = levitationVec.scale(delta);
        
        final int ticks = 5;
        
        Helper.log("Adjusting Client Player Position");
        
        int[] counter = {0};
        IPGlobal.CLIENT_TASK_LIST.addTask(() -> {
            if (player.isRemoved()) {
                return true;
            }
            
            if (GravityChangerInterface.invoker.getGravityDirection(player) != gravityDir) {
                return true;
            }
            
            if (counter[0] >= 5) {
                return true;
            }
            
            counter[0]++;
            
            double len = player.position().subtract(anchor).dot(levitationVec);
            if (len < -1 || len > 2) {
                // stop early
                return true;
            }
            
            double progress = ((double) counter[0]) / ticks;
            progress = TransformationManager.mapProgress(progress);
            
            Vec3 expectedPos = anchor.add(offset.scale(progress));
            
            Vec3 newPos = Helper.putCoordinate(player.position(), levitationDir.getAxis(),
                Helper.getCoordinate(expectedPos, levitationDir.getAxis())
            );
            
            Portal currentCollidingPortal = ((IEEntity) player).ip_getCollidingPortal();
            if (currentCollidingPortal != null) {
                Vec3 eyePos = McHelper.getEyePos(player);
                Vec3 newEyePos = newPos.add(McHelper.getEyeOffset(player));
                if (currentCollidingPortal.rayTrace(eyePos, newEyePos) != null) {
                    return true; // avoid going back into the portal
                }
            }
            
            player.setPosRaw(newPos.x, newPos.y, newPos.z);
            McHelper.updateBoundingBox(player);
            
            return false;
        });
        
    }
    
    public static class RemoteCallables {
        // living entities do position interpolation
        // it may interpolate into unloaded chunks and stuck
        // avoid position interpolation
        public static void updateEntityPos(
            ResourceKey<Level> dim,
            int entityId,
            Vec3 pos
        ) {
            ClientLevel world = ClientWorldLoader.getWorld(dim);
            
            Entity entity = world.getEntity(entityId);
            
            if (entity == null) {
                Helper.err("cannot find entity to update position");
                return;
            }
            
            // both of them are important for Minecart
            entity.setPos(pos);
            entity.lerpTo(
                pos.x, pos.y, pos.z,
                entity.getYRot(), entity.getXRot(),
                0
            );
            entity.setPos(pos);
        }
    }
}
