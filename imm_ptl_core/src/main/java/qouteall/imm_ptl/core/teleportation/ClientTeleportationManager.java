package qouteall.imm_ptl.core.teleportation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.PehkuiInterface;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientTeleportationManager {
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    private Vec3d moveStartPoint = null;
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
        RegistryKey<World> dimension,
        Vec3d pos,
        boolean forceAccept
    ) {
        if (!forceAccept) {
            if (isTeleportingFrequently()) {
                return;
            }
            // newly teleported by vanilla means
            if (client.player.age < 200) {
                return;
            }
        }
        if (client.player.world.getRegistryKey() != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
    }
    
    public void manageTeleportation(float tickDelta) {
        if (IPGlobal.disableTeleportation) {
            return;
        }
        
        isTeleportingFrame = false;
        
        if (client.world == null || client.player == null) {
            moveStartPoint = null;
        }
        else {
            //not initialized
            if (client.player.prevX == 0 && client.player.prevY == 0 && client.player.prevZ == 0) {
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
        ClientPlayerEntity player = client.player;
        
        Vec3d newHeadPos = getPlayerHeadPos(tickDelta);
        
        if (moveStartPoint.squaredDistanceTo(newHeadPos) > 1600) {
//            Helper.log("The Player is Moving Too Fast!");
            return false;
        }
        
        Pair<Portal, Vec3d> pair = CHelper.getClientNearbyPortals(32)
            .flatMap(portal -> {
                if (portal.canTeleportEntity(player)) {
                    Vec3d collidingPoint = portal.rayTrace(
                        moveStartPoint,
                        newHeadPos
                    );
                    if (collidingPoint != null) {
                        return Stream.of(new Pair<>(portal, collidingPoint));
                    }
                }
                return Stream.empty();
            })
            .min(Comparator.comparingDouble(
                p -> p.getRight().squaredDistanceTo(moveStartPoint)
            ))
            .orElse(null);
        
        if (pair != null) {
            Portal portal = pair.getLeft();
            Vec3d collidingPos = pair.getRight();
            
            client.getProfiler().push("portal_teleport");
            teleportPlayer(portal);
            client.getProfiler().pop();
            
            boolean allowOverlappedTeleport = portal.allowOverlappedTeleport();
            double adjustment = allowOverlappedTeleport ? -0.001 : 0.001;
            
            moveStartPoint = portal.transformPoint(collidingPos)
                .add(portal.getContentDirection().multiply(adjustment));
            //avoid teleporting through parallel portal due to floating point inaccuracy
            
            return true;
        }
        else {
            return false;
        }
    }
    
    public static Vec3d getPlayerHeadPos(float tickDelta) {
        return client.player.getCameraPosVec(tickDelta);
    }
    
    private void teleportPlayer(Portal portal) {
        if (tickTimeForTeleportation <= teleportTickTimeLimit) {
            Helper.log("Client player teleportation rejected");
            return;
        }
        
        lastTeleportGameTime = tickTimeForTeleportation;
        
        ClientPlayerEntity player = client.player;
        
        RegistryKey<World> toDimension = portal.dimensionTo;
        
        Vec3d oldEyePos = McHelper.getEyePos(player);
        
        Vec3d newEyePos = portal.transformPoint(oldEyePos);
        Vec3d newLastTickEyePos = portal.transformPoint(McHelper.getLastTickEyePos(player));
        
        ClientWorld fromWorld = client.world;
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        
        if (fromDimension != toDimension) {
            ClientWorld toWorld = ClientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
        }
    
        GravityChangerInterface.invoker.transformVelocityToWorld(player);// temporary workaround
        TransformationManager.managePlayerRotationAndChangeGravity(portal);
        portal.transformVelocity(player);
        GravityChangerInterface.invoker.transformVelocityToLocal(player);
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
        
        PehkuiInterface.invoker.onClientPlayerTeleported(portal);
        
        player.networkHandler.sendPacket(IPNetworkingClient.createCtsTeleport(
            fromDimension,
            oldEyePos,
            portal.getUuid()
        ));
        
        tickAfterTeleportation(player, newEyePos, newLastTickEyePos);
        
        McHelper.adjustVehicle(player);
        
        if (player.getVehicle() != null) {
            disableTeleportFor(40);
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
    
    private void forceTeleportPlayer(RegistryKey<World> toDimension, Vec3d destination) {
        Helper.log("force teleported " + toDimension + destination);
        
        ClientWorld fromWorld = client.world;
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        ClientPlayerEntity player = client.player;
        if (fromDimension == toDimension) {
            player.setPosition(
                destination.x,
                destination.y,
                destination.z
            );
            McHelper.adjustVehicle(player);
        }
        else {
            ClientWorld toWorld = ClientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, destination);
        }
        
        moveStartPoint = null;
        disableTeleportFor(20);
    }
    
    public void changePlayerDimension(
        ClientPlayerEntity player, ClientWorld fromWorld, ClientWorld toWorld, Vec3d newEyePos
    ) {
        Validate.isTrue(!WorldRenderInfo.isRendering());
        Validate.isTrue(!FrontClipping.isClippingEnabled);
        
        Entity vehicle = player.getVehicle();
        player.detach();
        
        RegistryKey<World> toDimension = toWorld.getRegistryKey();
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        
        ((IEClientPlayNetworkHandler) client.getNetworkHandler()).ip_setWorld(toWorld);
        
        fromWorld.removeEntity(player.getId(), Entity.RemovalReason.CHANGED_DIMENSION);
        
        player.world = toWorld;
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        ((IEEntity) player).portal_unsetRemoved();
        
        toWorld.addPlayer(player.getId(), player);
        
        client.world = toWorld;
        ((IEMinecraftClient) client).setWorldRenderer(
            ClientWorldLoader.getWorldRenderer(toDimension)
        );
        
        // don't know whether it's necessary
        MyGameRenderer.resetGlStates();
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (client.particleManager != null) {
            // avoid clearing all particles
            ((IEParticleManager) client.particleManager).ip_setWorld(toWorld);
        }
        
        client.getBlockEntityRenderDispatcher().setWorld(toWorld);
        
        IEGameRenderer gameRenderer = (IEGameRenderer) MinecraftClient.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(ClientWorldLoader
            .getDimensionRenderHelper(toDimension).lightmapTexture);
        
        if (vehicle != null) {
            Vec3d vehiclePos = new Vec3d(
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
            fromDimension.getValue(),
            toDimension.getValue(),
            tickTimeForTeleportation
        ));
        
        FogRendererContext.onPlayerTeleport(fromDimension, toDimension);
        
        O_O.onPlayerChangeDimensionClient(fromDimension, toDimension);
    }
    
    private void changePlayerMotionIfCollidingWithPortal() {
        ClientPlayerEntity player = client.player;
        
        Portal portal = ((IEEntity) player).getCollidingPortal();
        
        if (portal != null) {
            if (PortalExtension.get(portal).motionAffinity > 0) {
                changeMotion(player, portal);
            }
            else if (PortalExtension.get(portal).motionAffinity < 0) {
                if (player.getVelocity().length() > 0.7) {
                    changeMotion(player, portal);
                }
            }
        }
    }
    
    private void changeMotion(Entity player, Portal portal) {
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.multiply(1 + PortalExtension.get(portal).motionAffinity));
    }
    
    //foot pos, not eye pos
    public static void moveClientEntityAcrossDimension(
        Entity entity,
        ClientWorld newWorld,
        Vec3d newPos
    ) {
        ClientWorld oldWorld = (ClientWorld) entity.world;
        oldWorld.removeEntity(entity.getId(), Entity.RemovalReason.CHANGED_DIMENSION);
        entity.world = newWorld;
        entity.setPosition(newPos.x, newPos.y, newPos.z);
        newWorld.addEntity(entity.getId(), entity);
    }
    
    public void disableTeleportFor(int ticks) {
        teleportTickTimeLimit = tickTimeForTeleportation + ticks;
    }
    
    private static void tickAfterTeleportation(ClientPlayerEntity player, Vec3d newEyePos, Vec3d newLastTickEyePos) {
        // update collidingPortal
        McHelper.findEntitiesByBox(
            Portal.class,
            player.world,
            player.getBoundingBox(),
            10,
            portal -> true
        ).forEach(CollisionHelper::notifyCollidingPortals);
        
        CollisionHelper.tickClient();
        
        ((IEEntity) player).tickCollidingPortal(RenderStates.tickDelta);
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
    }
    
    private static void adjustPlayerPosition(ClientPlayerEntity player) {
        if (player.isSpectator()) {
            return;
        }
        
        Box boundingBox = player.getBoundingBox();
        Box bottomHalfBox = boundingBox.shrink(0, boundingBox.getYLength() / 2, 0);
        Iterable<VoxelShape> collisions = player.world.getBlockCollisions(
            player, bottomHalfBox
        );
        
        double maxY = player.getY();
        for (VoxelShape collision : collisions) {
            maxY = Math.max(
                collision.getBoundingBox().maxY,
                maxY
            );
        }
        
        double maxY1 = maxY;// must effectively final
        double delta = maxY - player.getY();
        
        if (delta <= 0) {
            return;
        }
        
        final int ticks = 5;
        
        double originalY = player.getY();
        
        Helper.log("Adjusting Client Player Position");
        
        int[] counter = {0};
        IPGlobal.clientTaskList.addTask(() -> {
            if (player.isRemoved()) {
                return true;
            }
            if (player.getY() < originalY - 1 || player.getY() > maxY1 + 1) {
                return true;
            }
            
            if (counter[0] >= 5) {
                return true;
            }
            
            counter[0]++;
            
            double progress = ((double) counter[0]) / ticks;
            progress = TransformationManager.mapProgress(progress);
            double newY = MathHelper.lerp(
                progress,
                originalY, maxY1
            );
            
            Vec3d newPos = new Vec3d(player.getX(), newY, player.getZ());
            
            Portal collidingPortal = ((IEEntity) player).getCollidingPortal();
            if (collidingPortal != null) {
                Vec3d eyePos = McHelper.getEyePos(player);
                Vec3d newEyePos = newPos.add(0, player.getStandingEyeHeight(), 0);
                if (collidingPortal.rayTrace(eyePos, newEyePos) != null) {
                    return true;//avoid going back into the portal
                }
            }
            
            player.setPos(newPos.x, newPos.y, newPos.z);
            McHelper.updateBoundingBox(player);
            
            return false;
        });
        
    }
    
    
}
