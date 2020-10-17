package com.qouteall.immersive_portals.teleportation;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
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
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    private WeakHashMap<Entity, Long> lastTeleportGameTime = new WeakHashMap<>();
    public boolean isFiringMyChangeDimensionEvent = false;
    public final WeakHashMap<ServerPlayerEntity, Pair<RegistryKey<World>, Vec3d>> lastPosition =
        new WeakHashMap<>();
    
    // The old teleport way does not recreate the entity
    // It's problematic because some AI-related fields contain world reference
    private static final boolean useOldTeleport = false;
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
        Portal.serverPortalTickSignal.connectWithWeakRef(
            this, (this_, portal) ->
                getEntitiesToTeleport(portal).forEach(entity -> {
                    tryToTeleportRegularEntity(portal, entity);
                })
        );
    }
    
    public static boolean shouldEntityTeleport(Portal portal, Entity entity) {
        return entity.world == portal.world &&
            portal.canTeleportEntity(entity) &&
            portal.isMovedThroughPortal(
                entity.getCameraPosVec(0),
                entity.getCameraPosVec(1).add(entity.getVelocity())
            );
    }
    
    public void tryToTeleportRegularEntity(Portal portal, Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            return;
        }
        if (entity instanceof Portal) {
            return;
        }
        if (entity.getVehicle() != null || doesEntityClutterContainPlayer(entity)) {
            return;
        }
        if (entity.removed) {
            return;
        }
        if (!entity.canUsePortals()) {
            return;
        }
        if (isJustTeleported(entity, 10)) {
            return;
        }
        //a new born entity may have last tick pos 0 0 0
        double motion = McHelper.lastTickPosOf(entity).squaredDistanceTo(entity.getPos());
        if (motion > 20) {
            return;
        }
        ModMain.serverTaskList.addTask(() -> {
            try {
                teleportRegularEntity(entity, portal);
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
            return true;
        });
    }
    
    private static Stream<Entity> getEntitiesToTeleport(Portal portal) {
        return portal.world.getEntitiesByClass(
            Entity.class,
            portal.getBoundingBox().expand(2),
            e -> true
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            entity -> shouldEntityTeleport(portal, entity)
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        RegistryKey<World> dimensionBefore,
        Vec3d oldEyePos,
        UUID portalId
    ) {
        recordLastPosition(player);
        
        Portal portal = findPortal(dimensionBefore, portalId);
        lastTeleportGameTime.put(player, McHelper.getServerGameTime());
        
        if (canPlayerTeleport(player, dimensionBefore, oldEyePos, portal)) {
            if (isTeleporting(player)) {
                Helper.log(player.toString() + "is teleporting frequently");
            }
            
            RegistryKey<World> dimensionTo = portal.dimensionTo;
            Vec3d newEyePos = portal.transformPoint(oldEyePos);
            
            teleportPlayer(player, dimensionTo, newEyePos);
            
            portal.onEntityTeleportedOnServer(player);
            
            PehkuiInterface.onServerEntityTeleported.accept(player, portal);
        }
        else {
            Helper.err(String.format(
                "Player cannot teleport through portal %s %s %s %s",
                player.getName().asString(),
                player.world.getRegistryKey(),
                player.getPos(),
                portal
            ));
        }
    }
    
    private Portal findPortal(RegistryKey<World> dimensionBefore, UUID portalId) {
        ServerWorld originalWorld = McHelper.getServer().getWorld(dimensionBefore);
        Entity portalEntity = originalWorld.getEntity(portalId);
        if (portalEntity == null) {
            portalEntity = GlobalPortalStorage.get(originalWorld).data
                .stream().filter(
                    p -> p.getUuid().equals(portalId)
                ).findFirst().orElse(null);
        }
        if (portalEntity == null) {
            return null;
        }
        if (portalEntity instanceof Portal) {
            return ((Portal) portalEntity);
        }
        return null;
    }
    
    public void recordLastPosition(ServerPlayerEntity player) {
        lastPosition.put(
            player,
            new Pair<>(player.world.getRegistryKey(), player.getPos())
        );
    }
    
    private boolean canPlayerTeleport(
        ServerPlayerEntity player,
        RegistryKey<World> dimensionBefore,
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
        RegistryKey<World> dimension,
        Vec3d pos
    ) {
        Vec3d playerPos = player.getPos();
        if (player.world.getRegistryKey() == dimension) {
            if (playerPos.squaredDistanceTo(pos) < 256) {
                return true;
            }
        }
        return McHelper.getNearbyPortals(player, 20)
            .filter(portal -> portal.dimensionTo == dimension)
            .map(portal -> portal.transformPoint(playerPos))
            .anyMatch(mappedPos -> mappedPos.squaredDistanceTo(pos) < 256);
    }
    
    public void teleportPlayer(
        ServerPlayerEntity player,
        RegistryKey<World> dimensionTo,
        Vec3d newEyePos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(dimensionTo);
        
        if (player.world.getRegistryKey() == dimensionTo) {
            McHelper.setEyePos(player, newEyePos, newEyePos);
            McHelper.updateBoundingBox(player);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
            ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
        }
        
        McHelper.adjustVehicle(player);
        player.networkHandler.syncWithPlayerPosition();
    }
    
    public void invokeTpmeCommand(
        ServerPlayerEntity player,
        RegistryKey<World> dimensionTo,
        Vec3d newPos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(dimensionTo);
        
        if (player.world.getRegistryKey() == dimensionTo) {
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
     * {@link ServerPlayerEntity#changeDimension(ServerWorld)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d newEyePos
    ) {
        NewChunkTrackingGraph.addAdditionalDirectTickets(player);
        
        teleportingEntities.add(player);
        
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            ((IEServerPlayerEntity) player).stopRidingWithoutTeleportRequest();
        }
        
        Vec3d oldPos = player.getPos();
        
        O_O.segregateServerPlayer(fromWorld, player);
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        player.world = toWorld;
        toWorld.onPlayerChangeDimension(player);
        
        toWorld.checkEntityChunkPos(player);
        
        player.interactionManager.setWorld(toWorld);
        
        if (vehicle != null) {
            Vec3d vehiclePos = new Vec3d(
                newEyePos.x,
                McHelper.getVehicleY(vehicle, player),
                newEyePos.z
            );
            changeEntityDimension(
                vehicle,
                toWorld.getRegistryKey(),
                vehiclePos.add(0, vehicle.getStandingEyeHeight(), 0),
                false
            );
            ((IEServerPlayerEntity) player).startRidingWithoutTeleportRequest(vehicle);
            McHelper.adjustVehicle(player);
        }
        
        Helper.log(String.format(
            "%s :: (%s %s %s %s)->(%s %s %s %s)",
            player.getName().asString(),
            fromWorld.getRegistryKey().getValue(),
            oldPos.getX(), oldPos.getY(), oldPos.getZ(),
            toWorld.getRegistryKey().getValue(),
            (int) player.getX(), (int) player.getY(), (int) player.getZ()
        ));
        
        O_O.onPlayerTravelOnServer(
            player,
            fromWorld.getRegistryKey(),
            toWorld.getRegistryKey()
        );
        
        //update advancements
        if (toWorld.getRegistryKey() == World.NETHER) {
            ((IEServerPlayerEntity) player).setEnteredNetherPos(player.getPos());
        }
        ((IEServerPlayerEntity) player).updateDimensionTravelAdvancements(fromWorld);
        
        
    }
    
    private void sendPositionConfirmMessage(ServerPlayerEntity player) {
        Packet packet = MyNetwork.createStcDimensionConfirm(
            player.world.getRegistryKey(),
            player.getPos()
        );
        
        player.networkHandler.sendPacket(packet);
    }
    
    private void tick() {
        teleportingEntities = new HashSet<>();
        long tickTimeNow = McHelper.getServerGameTime();
        ArrayList<ServerPlayerEntity> copiedPlayerList =
            McHelper.getCopiedPlayerList();
        if (tickTimeNow % 30 == 7) {
            for (ServerPlayerEntity player : copiedPlayerList) {
                updateForPlayer(tickTimeNow, player);
            }
        }
        copiedPlayerList.forEach(player -> {
            McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
                player.world,
                player.getPos(),
                Entity.class,
                32
            ).filter(
                entity -> !(entity instanceof ServerPlayerEntity)
            ).forEach(entity -> {
                McHelper.getGlobalPortals(entity.world).stream()
                    .filter(
                        globalPortal -> shouldEntityTeleport(globalPortal, entity)
                    )
                    .findFirst()
                    .ifPresent(
                        globalPortal -> tryToTeleportRegularEntity(globalPortal, entity)
                    );
            });
        });
    }
    
    private void updateForPlayer(long tickTimeNow, ServerPlayerEntity player) {
        // teleporting means dimension change
        // inTeleportationState means syncing position to client
        if (player.notInAnyWorld || player.teleporting) {
            lastTeleportGameTime.put(player, tickTimeNow);
            return;
        }
        Long lastTeleportGameTime =
            this.lastTeleportGameTime.getOrDefault(player, 0L);
        if (tickTimeNow - lastTeleportGameTime > 60) {
            sendPositionConfirmMessage(player);
            
            //for vanilla nether portal cooldown to work normally
            player.onTeleportationDone();
        }
        else {
            ((IEServerPlayNetworkHandler) player.networkHandler).cancelTeleportRequest();
        }
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        Validate.isTrue(!(entity instanceof ServerPlayerEntity));
        if (entity.world != portal.world) {
            Helper.err(String.format("Cannot teleport %s from %s through %s", entity, entity.world.getRegistryKey(), portal));
            return;
        }
        
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        if (currGameTime - lastTeleportGameTime < 3) {
            return;
        }
        this.lastTeleportGameTime.put(entity, currGameTime);
        
        if (entity.hasVehicle() || doesEntityClutterContainPlayer(entity)) {
            return;
        }
        
        Vec3d velocity = entity.getVelocity();
        
        List<Entity> passengerList = entity.getPassengerList();
        
        Vec3d newEyePos = portal.transformPoint(McHelper.getEyePos(entity));
        
        if (portal.dimensionTo != entity.world.getRegistryKey()) {
            entity = changeEntityDimension(entity, portal.dimensionTo, newEyePos, true);
            
            Entity newEntity = entity;
            
            passengerList.stream().map(
                e -> changeEntityDimension(e, portal.dimensionTo, newEyePos, true)
            ).collect(Collectors.toList()).forEach(e -> {
                e.startRiding(newEntity, true);
            });
        }
        
        McHelper.setEyePos(entity, newEyePos, newEyePos);
        McHelper.updateBoundingBox(entity);
        
        ((ServerWorld) entity.world).checkEntityChunkPos(entity);
        
        portal.transformVelocity(entity);
        
        portal.onEntityTeleportedOnServer(entity);
        
        PehkuiInterface.onServerEntityTeleported.accept(entity, portal);
        
        // a new entity may be created
        this.lastTeleportGameTime.put(entity, currGameTime);
    }
    
    /**
     * {@link Entity#moveToWorld(ServerWorld)}
     * Sometimes resuing the same entity object is problematic
     * because entity's AI related things may have world reference inside
     * These fields should also get changed but it's not easy
     * <p>
     * Recreating the entity is the vanilla way.
     * But it requires changing the corresponding entity reference on other places
     */
    public Entity changeEntityDimension(
        Entity entity,
        RegistryKey<World> toDimension,
        Vec3d newEyePos,
        boolean recreateEntity
    ) {
        ServerWorld fromWorld = (ServerWorld) entity.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        entity.detach();
        
        if (recreateEntity) {
            Entity oldEntity = entity;
            Entity newEntity;
            newEntity = entity.getType().create(toWorld);
            if (newEntity == null) {
                return oldEntity;
            }
            
            newEntity.copyFrom(oldEntity);
            McHelper.setEyePos(newEntity, newEyePos, newEyePos);
            McHelper.updateBoundingBox(newEntity);
            newEntity.setHeadYaw(oldEntity.getHeadYaw());
            
            // calling remove() will make chest minecart item duplicate
            oldEntity.removed = true;
            
            toWorld.onDimensionChanged(newEntity);
            
            return newEntity;
        }
        else {
            O_O.segregateServerEntity(fromWorld, entity);
            
            McHelper.setEyePos(entity, newEyePos, newEyePos);
            McHelper.updateBoundingBox(entity);
            
            entity.world = toWorld;
            
            toWorld.onDimensionChanged(entity);
            
            return entity;
        }
        
        
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
        RegistryKey<World> dimension
    ) {
        if (player.world.getRegistryKey() == dimension) {
            return;
        }
        double x = packet.getX(player.getX());
        double y = packet.getY(player.getY());
        double z = packet.getZ(player.getZ());
        Vec3d newPos = new Vec3d(x, y, z);
        if (canPlayerReachPos(player, dimension, newPos)) {
            recordLastPosition(player);
            teleportPlayer(player, dimension, newPos);
            Helper.log(String.format("accepted dubious move packet %s %s %s %s %s %s %s",
                player.world.getRegistryKey(), x, y, z, player.getX(), player.getY(), player.getZ()
            ));
        }
        else {
            Helper.log(String.format("ignored dubious move packet %s %s %s %s %s %s %s",
                player.world.getRegistryKey().getValue(), x, y, z, player.getX(), player.getY(), player.getZ()
            ));
        }
    }
}
