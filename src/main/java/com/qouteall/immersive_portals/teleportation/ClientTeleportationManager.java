package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.FogRendererContext;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
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

public class ClientTeleportationManager {
    MinecraftClient mc = MinecraftClient.getInstance();
    private long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    private Vec3d lastPlayerHeadPos = null;
    
    public ClientTeleportationManager() {
        ModMain.preRenderSignal.connectWithWeakRef(
            this, ClientTeleportationManager::manageTeleportation
        );
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, ClientTeleportationManager::tick
        );
    }
    
    private static void tick(ClientTeleportationManager this_) {
        this_.tickTimeForTeleportation++;
        this_.slowDownPlayerIfCollidingWithPortal();
        
        updateLight();
    }
    
    //fix light issue https://github.com/qouteall/ImmersivePortalsMod/issues/45
    //it's not an elegant solution
    //the issue could be caused by other things
    //TODO move this to another class
    private static void updateLight() {
        ClientWorld world = MinecraftClient.getInstance().world;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (world == null) {
            return;
        }
        if (player == null) {
            return;
        }
        if (world.getTime() % 233 == 34) {
            doUpdateLight(player);
        }
    }
    
    private static void doUpdateLight(ClientPlayerEntity player) {
        MinecraftClient.getInstance().getProfiler().push("my_light_update");
        MyClientChunkManager.updateLightStatus(player.world.getChunk(
            player.chunkX, player.chunkZ
        ));
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    public void acceptSynchronizationDataFromServer(
        DimensionType dimension,
        Vec3d pos,
        boolean forceAccept
    ) {
        if (!forceAccept) {
            if (isTeleportingFrequently()) {
                return;
            }
        }
        if (mc.player.dimension != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
        getOutOfLoadingScreen(dimension, pos);
    }
    
    private void manageTeleportation() {
        if (mc.world == null || mc.player == null) {
            lastPlayerHeadPos = null;
        }
        else {
            Vec3d currentHeadPos = mc.player.getCameraPosVec(MyRenderHelper.partialTicks);
            if (lastPlayerHeadPos != null) {
                if (lastPlayerHeadPos.squaredDistanceTo(currentHeadPos) > 100) {
                    Helper.err("The Player is Moving Too Fast!");
                }
                CHelper.getClientNearbyPortals(20).filter(
                    portal -> {
                        return mc.player.dimension == portal.dimension &&
                            portal.isTeleportable() &&
                            portal.isMovedThroughPortal(
                                lastPlayerHeadPos,
                                currentHeadPos
                            );
                    }
                ).findFirst().ifPresent(
                    portal -> onEntityGoInsidePortal(mc.player, portal)
                );
            }
    
            lastPlayerHeadPos = mc.player.getCameraPosVec(MyRenderHelper.partialTicks);
        }
    }
    
    private void onEntityGoInsidePortal(Entity entity, Portal portal) {
        if (entity instanceof ClientPlayerEntity) {
            assert entity.dimension == portal.dimension;
            teleportPlayer(portal);
        }
    }
    
    private void teleportPlayer(Portal portal) {
        if (isTeleportingFrequently()) {
            Helper.err("The Player is Teleporting Frequently");
        }
        lastTeleportGameTime = tickTimeForTeleportation;
        
        ClientPlayerEntity player = mc.player;
        
        DimensionType toDimension = portal.dimensionTo;
        
        if (mc.player.hasPassengers() || mc.player.hasVehicle()) {
            return;
        }
        
        Vec3d oldPos = player.getPos();
        
        Vec3d newPos = portal.applyTransformationToPoint(oldPos);
        Vec3d newLastTickPos = portal.applyTransformationToPoint(McHelper.lastTickPosOf(player));
        
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        if (fromDimension != toDimension) {
            ClientWorld toWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(toDimension);
    
            changePlayerDimension(player, fromWorld, toWorld, newPos);
        }
        
        player.setPosition(newPos.x, newPos.y, newPos.z);
        Helper.setPosAndLastTickPos(player, newPos, newLastTickPos);
    
        player.networkHandler.sendPacket(MyNetworkClient.createCtsTeleport(
            fromDimension,
            oldPos,
            portal.getUuid()
        ));
        
        amendChunkEntityStatus(player);
    
        //is it necessary?
        //doUpdateLight(player);
    }
    
    private boolean isTeleportingFrequently() {
        if (tickTimeForTeleportation - lastTeleportGameTime <= 2) {
            return true;
        }
        else {
            return false;
        }
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
            ClientWorld toWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, destination);
        }
    
        amendChunkEntityStatus(player);
    }
    
    /**
     * {@link ClientPlayNetworkHandler#onPlayerRespawn(PlayerRespawnS2CPacket)}
     */
    private void changePlayerDimension(
        ClientPlayerEntity player, ClientWorld fromWorld, ClientWorld toWorld, Vec3d destination
    ) {
    
        DimensionType toDimension = toWorld.dimension.getType();
        DimensionType fromDimension = fromWorld.dimension.getType();
    
        ClientPlayNetworkHandler workingNetHandler = ((IEClientWorld) fromWorld).getNetHandler();
        ClientPlayNetworkHandler fakedNetHandler = ((IEClientWorld) toWorld).getNetHandler();
        ((IEClientPlayNetworkHandler) workingNetHandler).setWorld(toWorld);
        ((IEClientPlayNetworkHandler) fakedNetHandler).setWorld(fromWorld);
        ((IEClientWorld) fromWorld).setNetHandler(fakedNetHandler);
        ((IEClientWorld) toWorld).setNetHandler(workingNetHandler);
    
        fromWorld.removeEntity(player.getEntityId());
        player.removed = false;
        player.world = toWorld;
    
        player.dimension = toDimension;
        player.setPosition(
            destination.x,
            destination.y,
            destination.z
        );//set pos and update bounding box
    
        toWorld.addPlayer(player.getEntityId(), player);
    
        mc.world = toWorld;
        ((IEMinecraftClient) mc).setWorldRenderer(
            CGlobal.clientWorldLoader.getWorldRenderer(toDimension)
        );
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
    
        if (mc.particleManager != null)
            mc.particleManager.setWorld(toWorld);
    
        BlockEntityRenderDispatcher.INSTANCE.setWorld(toWorld);
    
        IEGameRenderer gameRenderer = (IEGameRenderer) MinecraftClient.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(CGlobal.clientWorldLoader
            .getDimensionRenderHelper(toDimension).lightmapTexture);
    
    
        Helper.log(String.format(
            "Client Changed Dimension from %s to %s time: %s",
            fromDimension,
            toDimension,
            tickTimeForTeleportation
        ));
    
        //because the teleportation may happen before rendering
        //but after pre render info being updated
        MyRenderHelper.updatePreRenderInfo(MyRenderHelper.partialTicks);
    
        OFInterface.onPlayerTraveled.accept(fromDimension, toDimension);
    
        FogRendererContext.onPlayerTeleport(fromDimension, toDimension);

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
    
    private void getOutOfLoadingScreen(DimensionType dimension, Vec3d playerPos) {
        if (((IEMinecraftClient) mc).getCurrentScreen() instanceof DownloadingTerrainScreen) {
            Helper.err("Manually getting out of loading screen. The game is in abnormal state.");
            if (mc.player.dimension != dimension) {
                Helper.err("Manually fix dimension state while loading terrain");
                ClientWorld toWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
                changePlayerDimension(mc.player, mc.world, toWorld, playerPos);
            }
            mc.player.setPosition(playerPos.x, playerPos.y, playerPos.z);
            mc.openScreen(null);
        }
    }
    
    private void slowDownPlayerIfCollidingWithPortal() {
        boolean collidingWithPortal = !mc.player.world.getEntities(
            Portal.class,
            mc.player.getBoundingBox().expand(1),
            e -> !(e instanceof Mirror)
        ).isEmpty();
        
        if (collidingWithPortal) {
            slowDownIfTooFast(mc.player, 0.7);
        }
    }
    
    //if player is falling through looping portals, make it slower
    private void slowDownIfTooFast(ClientPlayerEntity player, double ratio) {
        if (player.getVelocity().length() > 0.7) {
            player.setVelocity(player.getVelocity().multiply(ratio));
        }
    }
}
