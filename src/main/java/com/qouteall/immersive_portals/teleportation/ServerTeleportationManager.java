package com.qouteall.immersive_portals.teleportation;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    private WeakHashMap<Entity, Long> lastTeleportGameTime = new WeakHashMap<>();
    public boolean isFiringMyChangeDimensionEvent = false;
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
        Portal.serverPortalTickSignal.connectWithWeakRef(
            this, (this_, portal) ->
                getEntitiesToTeleport(portal).forEach(entity -> {
                    tryToTeleportRegularEntity(portal, entity);
                })
        );
    }
    
    public void tryToTeleportRegularEntity(Portal portal, Entity entity) {
        if (!(entity instanceof ServerPlayerEntity)) {
            if (entity.getVehicle() != null || doesEntityClutterContainPlayer(entity)) {
                return;
            }
            ModMain.serverTaskList.addTask(() -> {
                teleportRegularEntity(entity, portal);
                return true;
            });
        }
    }
    
    private static Stream<Entity> getEntitiesToTeleport(Portal portal) {
        return portal.world.getEntities(
            Entity.class,
            portal.getBoundingBox().expand(2),
            e -> true
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            portal::shouldEntityTeleport
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        UUID portalId
    ) {
        ServerWorld originalWorld = McHelper.getServer().getWorld(dimensionBefore);
        Entity portalEntity = originalWorld.getEntity(portalId);
        if (portalEntity == null) {
            portalEntity = GlobalPortalStorage.get(originalWorld).data
                .stream().filter(
                    p -> p.getUuid().equals(portalId)
                ).findFirst().orElse(null);
        }
        lastTeleportGameTime.put(player, McHelper.getServerGameTime());
        
        if (canPlayerTeleport(player, dimensionBefore, posBefore, portalEntity)) {
            if (isTeleporting(player)) {
                Helper.err(player.toString() + "is teleporting frequently");
            }
            
            Portal portal = (Portal) portalEntity;
            
            DimensionType dimensionTo = portal.dimensionTo;
            Vec3d newPos = portal.applyTransformationToPoint(posBefore);
            
            teleportPlayer(player, dimensionTo, newPos);
            
            portal.onEntityTeleportedOnServer(player);
        }
        else {
            Helper.err(String.format(
                "Player cannot teleport through portal %s %s %s %s",
                player.getName().asString(),
                player.dimension,
                player.getPos(),
                portalEntity
            ));
        }
    }
    
    private boolean canPlayerTeleport(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        Entity portalEntity
    ) {
        if (player.getVehicle() != null) {
            return true;
        }
        return canPlayerReachPos(player, dimensionBefore, posBefore) &&
            portalEntity instanceof Portal &&
            ((Portal) portalEntity).getDistanceToPlane(posBefore) < 20;
    }
    
    public static boolean canPlayerReachPos(
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d pos
    ) {
        Vec3d playerPos = player.getPos();
        if (player.dimension == dimension) {
            if (playerPos.squaredDistanceTo(pos) < 256) {
                return true;
            }
        }
        return McHelper.getServerPortalsNearby(player, 20)
            .filter(portal -> portal.dimensionTo == dimension)
            .map(portal -> portal.applyTransformationToPoint(playerPos))
            .anyMatch(mappedPos -> mappedPos.squaredDistanceTo(pos) < 256);
    }
    
    public void teleportPlayer(
        ServerPlayerEntity player,
        DimensionType dimensionTo,
        Vec3d newPos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(dimensionTo);
        
        if (player.dimension == dimensionTo) {
            player.updatePosition(newPos.x, newPos.y, newPos.z);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newPos);
            ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
        }
        
        McHelper.adjustVehicle(player);
        player.networkHandler.syncWithPlayerPosition();
    }
    
    public void invokeTpmeCommand(
        ServerPlayerEntity player,
        DimensionType dimensionTo,
        Vec3d newPos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(dimensionTo);
        
        if (player.dimension == dimensionTo) {
            player.updatePosition(newPos.x, newPos.y, newPos.z);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newPos);
            sendPositionConfirmMessage(player);
        }
        
        player.networkHandler.requestTeleport(
            newPos.x,
            newPos.y,
            newPos.z,
            player.yaw,
            player.pitch
        );
        player.networkHandler.syncWithPlayerPosition();
        ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
        
    }
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d destination
    ) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            ((IEServerPlayerEntity) player).stopRidingWithoutTeleportRequest();
        }
        
        BlockPos oldPos = player.getBlockPos();
        
        teleportingEntities.add(player);
        
        O_O.segregateServerPlayer(fromWorld, player);
        
        player.updatePosition(destination.x, destination.y, destination.z);
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.onPlayerChangeDimension(player);
        
        toWorld.checkChunk(player);
        
        player.interactionManager.setWorld(toWorld);
        
        if (vehicle != null) {
            Vec3d vehiclePos = new Vec3d(
                destination.x,
                McHelper.getVehicleY(vehicle, player),
                destination.z
            );
            changeEntityDimension(
                vehicle,
                toWorld.dimension.getType(),
                vehiclePos
            );
            ((IEServerPlayerEntity) player).startRidingWithoutTeleportRequest(vehicle);
            McHelper.adjustVehicle(player);
        }
        
        Helper.log(String.format(
            "%s :: (%s %s %s %s)->(%s %s %s %s)",
            player.getName().asString(),
            fromWorld.dimension.getType(),
            oldPos.getX(), oldPos.getY(), oldPos.getZ(),
            toWorld.dimension.getType(),
            (int) player.getX(), (int) player.getY(), (int) player.getZ()
        ));
        
        O_O.onPlayerTravelOnServer(
            player,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
        
        //this is used for the advancement of "we need to go deeper"
        //and the advancement of travelling for long distance through nether
        if (toWorld.dimension.getType() == DimensionType.THE_NETHER) {
            //this is used for
            ((IEServerPlayerEntity) player).setEnteredNetherPos(player.getPos());
        }
        ((IEServerPlayerEntity) player).updateDimensionTravelAdvancements(fromWorld);
        
        
        
        GlobalPortalStorage.onPlayerLoggedIn(player);
    }
    
    private void sendPositionConfirmMessage(ServerPlayerEntity player) {
        Packet packet = MyNetwork.createStcDimensionConfirm(
            player.dimension,
            player.getPos()
        );
        
        player.networkHandler.sendPacket(packet);
    }
    
    private void tick() {
        teleportingEntities = new HashSet<>();
        long tickTimeNow = McHelper.getServerGameTime();
        ArrayList<ServerPlayerEntity> copiedPlayerList =
            McHelper.getCopiedPlayerList();
        if (tickTimeNow % 10 == 7) {
            for (ServerPlayerEntity player : copiedPlayerList) {
                if (!player.notInAnyWorld) {
                    Long lastTeleportGameTime =
                        this.lastTeleportGameTime.getOrDefault(player, 0L);
                    if (tickTimeNow - lastTeleportGameTime > 60) {
                        sendPositionConfirmMessage(player);
                    }
                    else {
                        ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
                    }
                }
            }
        }
        copiedPlayerList.forEach(player -> {
            McHelper.getEntitiesNearby(
                player,
                Entity.class,
                32
            ).filter(
                entity -> !(entity instanceof ServerPlayerEntity)
            ).forEach(entity -> {
                McHelper.getGlobalPortals(entity.world).stream()
                    .filter(
                        globalPortal -> globalPortal.shouldEntityTeleport(entity)
                    )
                    .findFirst()
                    .ifPresent(
                        globalPortal -> tryToTeleportRegularEntity(globalPortal, entity)
                    );
            });
        });
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        assert entity.dimension == portal.dimension;
        assert !(entity instanceof ServerPlayerEntity);
        
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        if (currGameTime - lastTeleportGameTime < 2) {
            return;
        }
        this.lastTeleportGameTime.put(entity, currGameTime);
        
        if (entity.hasVehicle() || doesEntityClutterContainPlayer(entity)) {
            return;
        }
        
        Vec3d newPos = portal.applyTransformationToPoint(entity.getPos());
        
        if (portal.dimensionTo != entity.dimension) {
            changeEntityDimension(entity, portal.dimensionTo, newPos);
        }
        
        entity.updatePosition(
            newPos.x, newPos.y, newPos.z
        );
        
        portal.onEntityTeleportedOnServer(entity);
    }
    
    /**
     * {@link Entity#changeDimension(DimensionType)}
     */
    public void changeEntityDimension(
        Entity entity,
        DimensionType toDimension,
        Vec3d destination
    ) {
        ServerWorld fromWorld = (ServerWorld) entity.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        entity.detach();
        
        O_O.segregateServerEntity(fromWorld, entity);
        
        entity.updatePosition(destination.x, destination.y, destination.z);
        
        entity.world = toWorld;
        entity.dimension = toDimension;
        toWorld.onDimensionChanged(entity);
    }
    
    private boolean doesEntityClutterContainPlayer(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return true;
        }
        List<Entity> passengerList = entity.getPassengerList();
        if (passengerList.isEmpty()) {
            return false;
        }
        return passengerList.stream().anyMatch(this::doesEntityClutterContainPlayer);
    }
    
    public boolean isJustTeleported(Entity entity, long valveTickTime) {
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        return currGameTime - lastTeleportGameTime < valveTickTime;
    }
    
    public void acceptDubiousMovePacket(
        ServerPlayerEntity player,
        PlayerMoveC2SPacket packet,
        DimensionType dimension
    ) {
        double x = packet.getX(player.getX());
        double y = packet.getY(player.getY());
        double z = packet.getZ(player.getZ());
        Vec3d newPos = new Vec3d(x, y, z);
        if (canPlayerReachPos(player, dimension, newPos)) {
            teleportPlayer(player, dimension, newPos);
            Helper.log("accepted dubious move packet");
        }
        else {
            Helper.log("dubious move packet is dubious");
        }
    }
}
