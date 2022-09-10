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
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
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
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.FogRendererContext;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientTeleportationManager {
    public static final Minecraft client = Minecraft.getInstance();
    public long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    private Vec3 moveStartPoint = null;
    private long teleportTickTimeLimit = 0;
    
    // for debug
    public static boolean isTeleportingTick = false;
    public static boolean isTeleportingFrame = false;
    
    private static final int teleportLimit = 2;
    
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
    
    public void manageTeleportation(float tickDelta) {
        if (IPGlobal.disableTeleportation) {
            return;
        }
        
        isTeleportingFrame = false;
        
        if (client.level == null || client.player == null) {
            moveStartPoint = null;
        }
        else {
            //not initialized
            if (client.player.xo == 0 && client.player.yo == 0 && client.player.zo == 0) {
                return;
            }
            
            client.getProfiler().push("ip_teleport");
            
            if (moveStartPoint != null) {
                for (int i = 0; i < teleportLimit; i++) {
                    boolean teleported = tryTeleport(tickDelta);
                    if (!teleported) {
                        break;
                    }
                    else {
                        if (i != 0) {
                            Helper.log("Nested teleport");
                        }
                    }
                }
            }
            
            moveStartPoint = getPlayerHeadPos(tickDelta);
            
            client.getProfiler().pop();
        }
    }
    
    private boolean tryTeleport(float tickDelta) {
        LocalPlayer player = client.player;
        
        Vec3 newHeadPos = getPlayerHeadPos(tickDelta);
        
        if (moveStartPoint.distanceToSqr(newHeadPos) > 1600) {
//            Helper.log("The Player is Moving Too Fast!");
            return false;
        }
        
        Tuple<Portal, Vec3> pair = CHelper.getClientNearbyPortals(32)
            .flatMap(portal -> {
                if (portal.canTeleportEntity(player)) {
                    Vec3 collidingPoint = portal.rayTrace(
                        moveStartPoint,
                        newHeadPos
                    );
                    if (collidingPoint != null) {
                        return Stream.of(new Tuple<>(portal, collidingPoint));
                    }
                }
                return Stream.empty();
            })
            .min(Comparator.comparingDouble(
                p -> p.getB().distanceToSqr(moveStartPoint)
            ))
            .orElse(null);
        
        if (pair != null) {
            Portal portal = pair.getA();
            Vec3 collidingPos = pair.getB();
            
            client.getProfiler().push("portal_teleport");
            teleportPlayer(portal);
            client.getProfiler().pop();
            
            boolean allowOverlappedTeleport = portal.allowOverlappedTeleport();
            double adjustment = allowOverlappedTeleport ? -0.001 : 0.001;
            
            moveStartPoint = portal.transformPoint(collidingPos)
                .add(portal.getContentDirection().scale(adjustment));
            //avoid teleporting through parallel portal due to floating point inaccuracy
            
            return true;
        }
        else {
            return false;
        }
    }
    
    public static Vec3 getPlayerHeadPos(float tickDelta) {
        return client.player.getEyePosition(tickDelta);
    }
    
    private void teleportPlayer(Portal portal) {
        if (tickTimeForTeleportation <= teleportTickTimeLimit) {
            Helper.log("Client player teleportation rejected");
            return;
        }
        
        lastTeleportGameTime = tickTimeForTeleportation;
        
        LocalPlayer player = client.player;
        Validate.isTrue(player != null);
        
        ResourceKey<Level> toDimension = portal.dimensionTo;
        
        Vec3 oldEyePos = McHelper.getEyePos(player);
        
        Vec3 newEyePos = portal.transformPoint(oldEyePos);
        Vec3 newLastTickEyePos = portal.transformPoint(McHelper.getLastTickEyePos(player));
        
        ClientLevel fromWorld = client.level;
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        
        if (fromDimension != toDimension) {
            ClientLevel toWorld = ClientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
        }
        
        Vec3 oldRealVelocity = McHelper.getWorldVelocity(player);
        TransformationManager.managePlayerRotationAndChangeGravity(portal);
        McHelper.setWorldVelocity(player, oldRealVelocity); // reset velocity change
        
        portal.transformVelocity(player);
        if (player.getVehicle() != null) {
            portal.transformVelocity(player.getVehicle());
        }
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
        
        PehkuiInterface.invoker.onClientPlayerTeleported(portal);
        
        player.connection.send(IPNetworkingClient.createCtsTeleport(
            fromDimension,
            oldEyePos,
            portal.getUUID()
        ));
        
        tickAfterTeleportation(player, newEyePos, newLastTickEyePos);
        
        McHelper.adjustVehicle(player);
        
        if (player.getVehicle() != null) {
            disableTeleportFor(10);
        }
        
        //because the teleportation may happen before rendering
        //but after pre render info being updated
        RenderStates.updatePreRenderInfo(RenderStates.tickDelta);
        
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
        
        moveStartPoint = null;
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
