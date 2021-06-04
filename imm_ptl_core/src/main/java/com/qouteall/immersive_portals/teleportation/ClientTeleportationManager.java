package com.qouteall.immersive_portals.teleportation;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalExtension;
import com.qouteall.immersive_portals.render.TransformationManager;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Comparator;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientTeleportationManager {
    MinecraftClient client = MinecraftClient.getInstance();
    public long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    private Vec3d moveStartPoint = null;
    private long teleportTickTimeLimit = 0;
    
    // for debug
    public static boolean isTeleportingTick = false;
    public static boolean isTeleportingFrame = false;
    
    private static final int teleportLimit = 2;
    
    public ClientTeleportationManager() {
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, ClientTeleportationManager::tick
        );
        
        ModMain.clientCleanupSignal.connectWithWeakRef(this, (this_) -> {
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
        if (Global.disableTeleportation) {
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
    
    private Vec3d getPlayerHeadPos(float tickDelta) {
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
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
        
        PehkuiInterface.onClientPlayerTeleported.accept(portal);
        
        player.networkHandler.sendPacket(MyNetworkClient.createCtsTeleport(
            fromDimension,
            oldEyePos,
            portal.getUuid()
        ));
        
        tickAfterTeleportation(player, newEyePos, newLastTickEyePos);
        
        amendChunkEntityStatus(player);
        
        McHelper.adjustVehicle(player);
        
        portal.transformVelocity(player);
        
        TransformationManager.onClientPlayerTeleported(portal);
        
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
        
        amendChunkEntityStatus(player);
    }
    
    public void changePlayerDimension(
        ClientPlayerEntity player, ClientWorld fromWorld, ClientWorld toWorld, Vec3d newEyePos
    ) {
        Entity vehicle = player.getVehicle();
        player.detach();
        
        RegistryKey<World> toDimension = toWorld.getRegistryKey();
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        
        ClientPlayNetworkHandler workingNetHandler = ((IEClientWorld) fromWorld).getNetHandler();
        ClientPlayNetworkHandler fakedNetHandler = ((IEClientWorld) toWorld).getNetHandler();
        ((IEClientPlayNetworkHandler) workingNetHandler).setWorld(toWorld);
        ((IEClientPlayNetworkHandler) fakedNetHandler).setWorld(fromWorld);
        ((IEClientWorld) fromWorld).setNetHandler(fakedNetHandler);
        ((IEClientWorld) toWorld).setNetHandler(workingNetHandler);
        
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
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (client.particleManager != null) {
//            client.particleManager.setWorld(toWorld);
            // avoid clearing all particles
            ((IEParticleManager) client.particleManager).mySetWorld(toWorld);
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
        
        
        OFInterface.onPlayerTraveled.accept(fromDimension, toDimension);
        
        FogRendererContext.onPlayerTeleport(fromDimension, toDimension);
        
        O_O.onPlayerChangeDimensionClient(fromDimension, toDimension);
    }
    
    // TODO remove
    private void amendChunkEntityStatus(Entity entity) {
//        WorldChunk worldChunk1 = entity.world.getWorldChunk(new BlockPos(entity.getPos()));
//        Chunk chunk2 = entity.world.getChunk(entity.chunkX, entity.chunkZ);
//        removeEntityFromChunk(entity, worldChunk1);
//        if (chunk2 instanceof WorldChunk) {
//            removeEntityFromChunk(entity, ((WorldChunk) chunk2));
//        }
//        worldChunk1.addEntity(entity);
    }

//    private void removeEntityFromChunk(Entity entity, WorldChunk worldChunk) {
//        for (TypeFilterableList<Entity> section : worldChunk.getEntitySectionArray()) {
//            section.remove(entity);
//        }
//    }
    
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
        
        player.tick();
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
    }
    
    private static void adjustPlayerPosition(ClientPlayerEntity player) {
        if (player.isSpectator()) {
            return;
        }
        
        Box boundingBox = player.getBoundingBox();
        Box bottomHalfBox = boundingBox.shrink(0, boundingBox.getYLength() / 2, 0);
        Stream<VoxelShape> collisions = player.world.getBlockCollisions(
            player, bottomHalfBox
        );
        double maxCollisionY = collisions.mapToDouble(s -> s.getBoundingBox().maxY)
            .max().orElse(player.getY());
        
        double delta = maxCollisionY - player.getY();
        
        if (delta <= 0) {
            return;
        }
        
        final int ticks = 5;
        
        double originalY = player.getY();
        
        int[] counter = {0};
        ModMain.clientTaskList.addTask(() -> {
            if (player.isRemoved()) {
                return true;
            }
            if (player.getY() < originalY - 1 || player.getY() > maxCollisionY + 1) {
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
                originalY, maxCollisionY
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
