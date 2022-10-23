package qouteall.imm_ptl.core.teleportation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.*;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.network.PacketRedirectionClient;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.ClientPortalAnimationManagement;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.FogRendererContext;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Vec2d;

import java.util.Comparator;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientTeleportationManager {
    public static final Minecraft client = Minecraft.getInstance();
    public long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    private long teleportTickTimeLimit = 0;
    
    private Vec3 lastPlayerEyePos = null;
    private long lastRecordStableTickTime = 0;
    private float lastRecordStablePartialTicks = 0;
    
    // for debug
    public static boolean isTeleportingTick = false;
    public static boolean isTeleportingFrame = false;
    
    private static final int teleportLimitPerFrame = 2;
    
    private static long teleportationCounter = 0;
    
    public ClientTeleportationManager() {
        IPGlobal.postClientTickSignal.connectWithWeakRef(
            this, ClientTeleportationManager::tick
        );
        
        IPGlobal.clientCleanupSignal.connectWithWeakRef(this, (this_) -> {
            this_.disableTeleportFor(40);
        });
    }
    
    private void tick() {
        tickTimeForTeleportation++;
        changePlayerMotionIfCollidingWithPortal();
        
        isTeleportingTick = false;
    }
    
    public void acceptSynchronizationDataFromServer(
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
        if (client.player.level.dimension() != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
    }
    
    public void manageTeleportation() {
        if (IPGlobal.disableTeleportation) {
            return;
        }
        
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
            portal -> portal.animation.updateClientState(portal, teleportationCounter)
        );
        
        // the real partial ticks (not from stable timer)
        float realPartialTicks = RenderStates.tickDelta;
        
        double timePassedSinceLastUpdate =
            (int) (StableClientTimer.getStableTickTime() - lastRecordStableTickTime)
                + (StableClientTimer.getStablePartialTicks()) - lastRecordStablePartialTicks;
        if (timePassedSinceLastUpdate < 0) {
            Helper.err("time flows backward?");
        }
        else if (timePassedSinceLastUpdate == 0) {
            return;
        }
        
        if (lastPlayerEyePos != null) {
            for (int i = 0; i < teleportLimitPerFrame; i++) {
                boolean teleported = tryTeleport(realPartialTicks, timePassedSinceLastUpdate);
                if (!teleported) {
                    break;
                }
                else {
                    if (i != 0) {
                        Helper.log("The client player made a combo-teleport");
                    }
                }
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
    
    private boolean tryTeleport(float partialTicks, double timePassedSinceLastUpdate) {
        LocalPlayer player = client.player;
        
        Vec3 newEyePos = getPlayerEyePos(partialTicks);
        
        if (lastPlayerEyePos.distanceToSqr(newEyePos) > 1600) {
//            Helper.log("The Player is Moving Too Fast!");
            return false;
        }
        
        long currentGameTime = client.level.getGameTime();
        
        TeleportationUtil.Teleportation rec = CHelper.getClientNearbyPortals(32)
            .flatMap(portal -> {
                if (portal.canTeleportEntity(player)) {
                    portal.animation.updateClientState(portal, teleportationCounter);
                    
                    // Separately handle dynamic teleportation and static teleportation.
                    // Although the dynamic teleportation code can handle static teleportation.
                    // I want the dynamic teleportation bugs to not affect static teleportation.
                    if (portal.animation.clientLastPortalStateCounter == teleportationCounter - 1
                        && portal.animation.clientLastPortalState != null
                    ) {
                        // the portal is running a real animation
                        PortalState currentState = portal.animation.clientCurrentPortalState;
                        Validate.isTrue(currentState != null);
                        
                        TeleportationUtil.Teleportation teleportation =
                            TeleportationUtil.checkDynamicTeleportation(
                                portal,
                                portal.animation.clientLastPortalState,
                                currentState,
                                lastPlayerEyePos,
                                newEyePos,
                                timePassedSinceLastUpdate
                            );
                        
                        if (teleportation != null) {
                            return Stream.of(teleportation);
                        }
                    }
                    else {
                        // the portal is static
                        TeleportationUtil.Teleportation teleportation =
                            TeleportationUtil.checkStaticTeleportation(
                                portal,
                                lastPlayerEyePos,
                                newEyePos,
                                timePassedSinceLastUpdate
                            );
                        if (teleportation != null) {
                            return Stream.of(teleportation);
                        }
                    }
                }
                return Stream.empty();
            })
            .min(Comparator.comparingDouble(
                p -> p.collidingPos().distanceToSqr(lastPlayerEyePos)
            ))
            .orElse(null);
        
        if (rec != null) {
            Portal portal = rec.portal();
            Vec3 collidingPos = rec.collidingPos();
            
            client.getProfiler().push("portal_teleport");
            teleportPlayer(rec);
            client.getProfiler().pop();
            
            boolean allowOverlappedTeleport = portal.allowOverlappedTeleport();
            double adjustment = allowOverlappedTeleport ? -0.001 : 0.001;
            
            lastPlayerEyePos = portal.transformPoint(collidingPos)
                .add(portal.getContentDirection().scale(adjustment));
            //avoid teleporting through parallel portal due to floating point inaccuracy
            
            return true;
        }
        else {
            return false;
        }
    }
    
    public static Vec3 getPlayerEyePos(float tickDelta) {
        return client.player.getEyePosition(tickDelta);
    }
    
    private void teleportPlayer(
        TeleportationUtil.Teleportation teleportation
    ) {
        Portal portal = teleportation.portal();
        
        if (tickTimeForTeleportation <= teleportTickTimeLimit) {
            Helper.log("Client player teleportation rejected");
            return;
        }
        
        lastTeleportGameTime = tickTimeForTeleportation;
        
        LocalPlayer player = client.player;
        Validate.isTrue(player != null);
        
        ResourceKey<Level> toDimension = portal.dimensionTo;
        float tickDelta = RenderStates.tickDelta;
        
        Vec3 thisTickEyePos = McHelper.getEyePos(player);
        Vec3 lastTickEyePos = McHelper.getLastTickEyePos(player);
        
        Tuple<Vec3, Vec3> t = TeleportationUtil.getTransformedLastTickPosAndCurrentTickPos(
            teleportation, lastTickEyePos, thisTickEyePos
        );
        
        Vec3 newEyePos = t.getB();
        Vec3 newLastTickEyePos = t.getA();
        
        ClientLevel fromWorld = client.level;
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        
        if (fromDimension != toDimension) {
            ClientLevel toWorld = ClientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
        }
        
        Vec3 oldRealVelocity = McHelper.getWorldVelocity(player);
        TransformationManager.managePlayerRotationAndChangeGravity(portal);
        McHelper.setWorldVelocity(player, oldRealVelocity); // reset velocity change
        
        // subtract this side's portal point velocity
        McHelper.setWorldVelocity(player, McHelper.getWorldVelocity(player).subtract(teleportation.thisSidePortalPointVelocity()));
        
        portal.transformVelocity(player);
        
        // add other side's portal point velocity
        McHelper.setWorldVelocity(player, McHelper.getWorldVelocity(player).add(teleportation.otherSidePortalPointVelocity()));
        
        if (player.getVehicle() != null) {
            portal.transformVelocity(player.getVehicle());
        }
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
        
        PehkuiInterface.invoker.onClientPlayerTeleported(portal);
        
        player.connection.send(IPNetworkingClient.createCtsTeleport(
            fromDimension,
            lastTickEyePos,
            portal.getUUID()
        ));
        
        tickAfterTeleportation(player, newEyePos, newLastTickEyePos);
        
        McHelper.adjustVehicle(player);
        
        if (player.getVehicle() != null) {
            disableTeleportFor(10);
        }
        
        //because the teleportation may happen before rendering
        //but after pre render info being updated
        RenderStates.updatePreRenderInfo(tickDelta);
        
        Helper.log(String.format("Client Teleported %s %s", portal, tickTimeForTeleportation));
        
        isTeleportingTick = true;
        isTeleportingFrame = true;
        
        if (PortalExtension.get(portal).adjustPositionAfterTeleport) {
            adjustPlayerPosition(player);
        }
        
        MyGameRenderer.vanillaTerrainSetupOverride = 1;
    }
    
    
    public boolean isTeleportingFrequently() {
        return (tickTimeForTeleportation - lastTeleportGameTime <= 20) ||
            (tickTimeForTeleportation <= teleportTickTimeLimit);
    }
    
    private void forceTeleportPlayer(ResourceKey<Level> toDimension, Vec3 destination) {
        Helper.log("force teleported " + toDimension + destination);
        
        ClientLevel fromWorld = client.level;
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        LocalPlayer player = client.player;
        if (fromDimension == toDimension) {
            player.setPos(
                destination.x,
                destination.y,
                destination.z
            );
            McHelper.adjustVehicle(player);
        }
        else {
            ClientLevel toWorld = ClientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, destination);
        }
        
        lastPlayerEyePos = null;
        disableTeleportFor(20);
        
        RenderStates.updatePreRenderInfo(RenderStates.tickDelta);
        MyGameRenderer.vanillaTerrainSetupOverride = 1;
    }
    
    public void changePlayerDimension(
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
        
        player.level = toWorld;
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        ((IEEntity) player).portal_unsetRemoved();
        
        toWorld.addPlayer(player.getId(), player);
        
        IEGameRenderer gameRenderer = (IEGameRenderer) Minecraft.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(ClientWorldLoader
            .getDimensionRenderHelper(toDimension).lightmapTexture);
        
        client.level = toWorld;
        ((IEMinecraftClient) client).setWorldRenderer(
            ClientWorldLoader.getWorldRenderer(toDimension)
        );
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (client.particleEngine != null) {
            // avoid clearing all particles
            ((IEParticleManager) client.particleEngine).ip_setWorld(toWorld);
        }
        
        client.getBlockEntityRenderDispatcher().setLevel(toWorld);
        
        if (vehicle != null) {
            Vec3 vehiclePos = new Vec3(
                newEyePos.x,
                McHelper.getVehicleY(vehicle, player),
                newEyePos.z
            );
            moveClientEntityAcrossDimension(
                vehicle, toWorld,
                vehiclePos
            );
            player.startRiding(vehicle, true);
        }
        
        Helper.log(String.format(
            "Client Changed Dimension from %s to %s time: %s",
            fromDimension.location(),
            toDimension.location(),
            tickTimeForTeleportation
        ));
        
        FogRendererContext.onPlayerTeleport(fromDimension, toDimension);
        
        O_O.onPlayerChangeDimensionClient(fromDimension, toDimension);
    }
    
    private void changePlayerMotionIfCollidingWithPortal() {
        LocalPlayer player = client.player;
        
        Portal portal = ((IEEntity) player).getCollidingPortal();
        
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
    
    private void changeMotion(Entity player, Portal portal) {
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.scale(1 + PortalExtension.get(portal).motionAffinity));
    }
    
    //foot pos, not eye pos
    public static void moveClientEntityAcrossDimension(
        Entity entity,
        ClientLevel newWorld,
        Vec3 newPos
    ) {
        ClientLevel oldWorld = (ClientLevel) entity.level;
        oldWorld.removeEntity(entity.getId(), Entity.RemovalReason.CHANGED_DIMENSION);
        entity.level = newWorld;
        entity.setPos(newPos.x, newPos.y, newPos.z);
        newWorld.putNonPlayerEntity(entity.getId(), entity);
    }
    
    public void disableTeleportFor(int ticks) {
        teleportTickTimeLimit = tickTimeForTeleportation + ticks;
    }
    
    private static void tickAfterTeleportation(LocalPlayer player, Vec3 newEyePos, Vec3 newLastTickEyePos) {
        // update collidingPortal
        McHelper.findEntitiesByBox(
            Portal.class,
            player.level,
            player.getBoundingBox(),
            10,
            portal -> true
        ).forEach(CollisionHelper::notifyCollidingPortals);
        
        CollisionHelper.tickClient();
        
        ((IEEntity) player).tickCollidingPortal(RenderStates.tickDelta);
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
    }
    
    private static void adjustPlayerPosition(LocalPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        
        // TODO cut bounding box by colliding portal
        AABB playerBoundingBox = player.getBoundingBox();
        
        Direction gravityDir = GravityChangerInterface.invoker.getGravityDirection(player);
        Direction levitationDir = gravityDir.getOpposite();
        Vec3 eyeOffset = GravityChangerInterface.invoker.getEyeOffset(player);
        
        AABB bottomHalfBox = playerBoundingBox.contract(eyeOffset.x / 2, eyeOffset.y / 2, eyeOffset.z / 2);
        Iterable<VoxelShape> collisions = player.level.getBlockCollisions(
            player, bottomHalfBox
        );
        
        AABB collisionUnion = null;
        for (VoxelShape collision : collisions) {
            AABB collisionBoundingBox = collision.bounds();
            if (collisionUnion == null) {
                collisionUnion = collisionBoundingBox;
            }
            else {
                collisionUnion = collisionUnion.minmax(collisionBoundingBox);
            }
        }
        
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
        IPGlobal.clientTaskList.addTask(() -> {
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
            
            Portal collidingPortal = ((IEEntity) player).getCollidingPortal();
            if (collidingPortal != null) {
                Vec3 eyePos = McHelper.getEyePos(player);
                Vec3 newEyePos = newPos.add(McHelper.getEyeOffset(player));
                if (collidingPortal.rayTrace(eyePos, newEyePos) != null) {
                    return true;//avoid going back into the portal
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
            entity.lerpTo(
                pos.x, pos.y, pos.z,
                entity.getYRot(), entity.getXRot(),
                0, false
            );
            entity.setPos(pos);
        }
    }
}
