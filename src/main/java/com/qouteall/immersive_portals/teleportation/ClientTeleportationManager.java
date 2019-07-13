package com.qouteall.immersive_portals.teleportation;

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

import java.util.function.BooleanSupplier;

public class ClientTeleportationManager {
    MinecraftClient mc = MinecraftClient.getInstance();
    public BooleanSupplier shouldIgnorePositionPacket = () -> false;
    
    public ClientTeleportationManager() {
        Portal.clientPortalTickSignal.connect(portal -> {
            portal.world.getEntities(
                Entity.class,
                portal.getPortalCollisionBox()
            ).stream().filter(
                e -> !(e instanceof Portal)
            ).filter(
                portal::shouldEntityTeleport
            ).forEach(
                e -> onEntityGoInsidePortal(e, portal)
            );
        });
    }
    
    public void acceptSynchronizationDataFromServer(DimensionType dimension, Vec3d pos) {
        //TODO server send data to sync
        if (mc.player.dimension != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
    }
    
    private void onEntityGoInsidePortal(Entity entity, Portal portal) {
        ModMain.clientTaskList.addTask(() -> {
            if (entity instanceof ClientPlayerEntity) {
                teleportPlayer(portal.getEntityId());
            }
            return true;
        });
        
    }
    
    private void teleportPlayer(int portalId) {
        ClientPlayerEntity player = mc.player;
        
        Entity portalEntity = mc.world.getEntityById(portalId);
        if (!(portalEntity instanceof Portal)) {
            Helper.err("client cannot find portal " + portalId);
            return;
        }
        Portal portal = (Portal) portalEntity;
        DimensionType toDimension = portal.dimensionTo;
        
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
        
        long teleportTime = System.nanoTime();
        shouldIgnorePositionPacket = () -> System.nanoTime() - teleportTime < 5000000000L;
        
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
    }
}
