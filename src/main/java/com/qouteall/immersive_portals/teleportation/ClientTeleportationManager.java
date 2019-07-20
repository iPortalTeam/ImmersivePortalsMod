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
import net.minecraft.util.TypeFilterableList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientTeleportationManager {
    MinecraftClient mc = MinecraftClient.getInstance();
    private long lastTeleportGameTime = 0;
    //public BooleanSupplier shouldSendPositionPacket = () -> true;
    
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
            assert entity.dimension == portal.dimension;
            teleportPlayer(portal);
        }
    }
    
    private void teleportPlayer(Portal portal) {
        long currGameTime = mc.world.getTime();
        if (currGameTime - lastTeleportGameTime < 3) {
            return;
        }
        lastTeleportGameTime = currGameTime;
        
        ClientPlayerEntity player = mc.player;
        
        DimensionType toDimension = portal.dimensionTo;
        
        if (!portal.shouldEntityTeleport(mc.player)) {
            return;
        }
        
        Vec3d oldPos = player.getPos();
        
        Vec3d newPos = portal.applyTransformationToPoint(oldPos);
        Vec3d newLastTickPos = portal.applyTransformationToPoint(Helper.lastTickPosOf(player));
        
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        if (fromDimension != toDimension) {
            ClientWorld toWorld = Globals.clientWorldLoader.getOrCreateFakedWorld(toDimension);
    
            changePlayerDimension(player, fromWorld, toWorld, newPos);
        }
        
        player.setPosition(newPos.x, newPos.y, newPos.z);
        Helper.setPosAndLastTickPos(player, newPos, newLastTickPos);
        
        player.networkHandler.sendPacket(MyNetwork.createCtsTeleport(
            fromDimension,
            oldPos,
            portal.getEntityId()
        ));
        
        amendChunkEntityStatus(player);
        
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
    
    private void amendChunkEntityStatus(Entity entity) {
        WorldChunk worldChunk1 = entity.world.getWorldChunk(entity.getBlockPos());
        Chunk chunk2 = entity.world.getChunk(entity.chunkX, entity.chunkZ);
        removeEntityFromChunk(entity, worldChunk1);
        if (chunk2 instanceof WorldChunk) {
            removeEntityFromChunk(entity, ((WorldChunk) chunk2));
        }
        worldChunk1.addEntity(entity);
    }
    
    private void removeEntityFromChunk(Entity entity, WorldChunk worldChunk) {
        for (TypeFilterableList<Entity> section : worldChunk.getEntitySectionArray()) {
            section.remove(entity);
        }
    }
}
