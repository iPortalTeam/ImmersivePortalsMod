package com.qouteall.immersive_portals.teleportation;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.packet.PlayerRespawnS2CPacket;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientTeleportationManager {
    MinecraftClient mc = MinecraftClient.getInstance();
    private long lastTeleportGameTime = 0;
    
    public ClientTeleportationManager() {
        ModMain.preRenderSignal.connectWithWeakRef(
            this, ClientTeleportationManager::manageTeleportation
        );
    }
    
    public void acceptSynchronizationDataFromServer(DimensionType dimension, Vec3d pos) {
        if (mc.player.dimension != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
    }
    
    private void manageTeleportation() {
        if (mc.world != null) {
            Streams.stream(mc.world.getEntities())
                .filter(e -> e instanceof Portal)
                .flatMap(
                    entityPortal -> getEntitiesToTeleport(
                        ((Portal) entityPortal)
                    ).map(
                        entity -> ((Runnable) () -> {
                            onEntityGoInsidePortal(entity, ((Portal) entityPortal));
                        })
                    )
                )
                .collect(Collectors.toCollection(ArrayDeque::new))
                .forEach(Runnable::run);
        }
    }
    
    private Stream<Entity> getEntitiesToTeleport(Portal portal) {
        return portal.world.getEntities(
            Entity.class,
            portal.getPortalCollisionBox()
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            portal::shouldEntityTeleport
        );
    }
    
    private void onEntityGoInsidePortal(Entity entity, Portal portal) {
        if (entity instanceof ClientPlayerEntity) {
            teleportPlayer(portal.getEntityId());
        }
    }
    
    private void teleportPlayer(int portalId) {
        long currTime = System.nanoTime();
        if (currTime - lastTeleportGameTime < Helper.secondToNano(0.1)) {
            Helper.err("teleport frequency so high");
            return;
        }
        lastTeleportGameTime = currTime;
        
        ClientPlayerEntity player = mc.player;
        
        Entity portalEntity = mc.world.getEntityById(portalId);
        if (!(portalEntity instanceof Portal)) {
            Helper.err("client cannot find portal " + portalId);
            return;
        }
    
        Portal portal = (Portal) portalEntity;
        DimensionType toDimension = portal.dimensionTo;
    
        if (!portal.shouldEntityTeleport(mc.player)) {
            return;
        }
        
        Vec3d newPos = portal.applyTransformationToPoint(player.getPos());
        Vec3d newLastTickPos = portal.applyTransformationToPoint(Helper.lastTickPosOf(player));
        
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        if (fromDimension != toDimension) {
            ClientWorld toWorld = Globals.clientWorldLoader.getOrCreateFakedWorld(toDimension);
    
            changePlayerDimension(player, fromWorld, toWorld, newPos);
        }
        
        player.setPosition(newPos.x, newPos.y, newPos.z);
        Helper.setPosAndLastTickPos(player, newPos, newLastTickPos);
        
        player.networkHandler.sendPacket(MyNetwork.createCtsTeleport(portal.getEntityId()));
    
    }
    
    private void forceTeleportPlayer(DimensionType toDimension, Vec3d destination) {
        Helper.log("force teleported " + toDimension + destination);
        
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        ClientPlayerEntity player = mc.player;
        if (fromDimension == toDimension) {
            player.setPosition(
                destination.x,
                destination.y,
                destination.z
            );
        }
        else {
            ClientWorld toWorld = Globals.clientWorldLoader.getOrCreateFakedWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, destination);
        }
    }
    
    /**
     * {@link ClientPlayNetworkHandler#onPlayerRespawn(PlayerRespawnS2CPacket)}
     */
    private void changePlayerDimension(
        ClientPlayerEntity player, ClientWorld fromWorld, ClientWorld toWorld, Vec3d destination
    ) {
    
        ClientPlayNetworkHandler workingNetHandler = ((IEClientWorld) fromWorld).getNetHandler();
        ClientPlayNetworkHandler fakedNetHandler = ((IEClientWorld) toWorld).getNetHandler();
        ((IEClientPlayNetworkHandler) workingNetHandler).setWorld(toWorld);
        ((IEClientPlayNetworkHandler) fakedNetHandler).setWorld(fromWorld);
        ((IEClientWorld) fromWorld).setNetHandler(fakedNetHandler);
        ((IEClientWorld) toWorld).setNetHandler(workingNetHandler);
        
        fromWorld.removeEntity(player.getEntityId());
        player.removed = false;
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        player.x = destination.x;
        player.y = destination.y;
        player.z = destination.z;
        
        toWorld.addPlayer(player.getEntityId(), player);
        
        mc.world = toWorld;
        mc.worldRenderer = Globals.clientWorldLoader.getWorldRenderer(toWorld.dimension.getType());
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (mc.particleManager != null)
            mc.particleManager.setWorld(toWorld);
        
        BlockEntityRenderDispatcher.INSTANCE.setWorld(toWorld);
    
        Helper.log(String.format(
            "Client Changed Dimension from %s to %s",
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        ));
    
        Helper.log("Portal Number Near Player Now" +
            Helper.getEntitiesNearby(mc.player, Portal.class, 10).count()
        );
        
    }
}
