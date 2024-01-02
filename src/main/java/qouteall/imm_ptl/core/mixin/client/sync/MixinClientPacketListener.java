package qouteall.imm_ptl.core.mixin.client.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.network.ImmPtlNetworkConfig;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.CountDownInt;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener implements IEClientPlayNetworkHandler {
    private static CountDownInt LOG_LIMIT = new CountDownInt(20);
    
    @Shadow
    private ClientLevel level;
    
    @Mutable
    @Shadow
    @Final
    private Map<UUID, PlayerInfo> playerInfoMap;
    
    @Shadow
    public abstract void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket entityPassengersSetS2CPacket_1);
    
    @Shadow
    protected abstract void applyLightData(int x, int z, ClientboundLightUpdatePacketData data);
    
    @Shadow
    @Final
    private static Logger LOGGER;
    
    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();
    
    @Shadow
    protected abstract void enableChunkLight(LevelChunk chunk, int x, int z);
    
    @Override
    public void ip_setWorld(ClientLevel world) {
        this.level = world;
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onInit(
        Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfo ci
    ) {
        isReProcessingPassengerPacket = false;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/ClientPacketListener;handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onProcessingPositionPacket(
        ClientboundPlayerPositionPacket packet,
        CallbackInfo ci
    ) {
        if (!ImmPtlNetworkConfig.doesServerHaveImmPtl()) {
            return;
        }
        
        ResourceKey<Level> packetDim = ((IEPlayerPositionLookS2CPacket) packet).ip_getPlayerDimension();
        
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        Level playerWorld = player.level();
        
        if (packetDim != playerWorld.dimension()) {
            LOGGER.info(
                "[ImmPtl] Client accepted position packet in another dimension. Packet: {} {} {} {}. Player: {} {} {} {}",
                packetDim.location(), packet.getX(), packet.getY(), packet.getZ(),
                playerWorld.dimension().location(), player.getX(), player.getY(), player.getZ()
            );
            
            ClientTeleportationManager.forceTeleportPlayer(
                packetDim,
                new Vec3(packet.getX(), packet.getY(), packet.getZ())
            );

//            ClientTeleportationManager.disableTeleportFor(2);
        }
        
        LOGGER.info(
            "[ImmPtl] Client accepted position packet {} {} {} {}",
            packetDim.location(), packet.getX(), packet.getY(), packet.getZ()
        );
    }
    
    private boolean isReProcessingPassengerPacket;
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/ClientPacketListener;handleSetEntityPassengersPacket(Lnet/minecraft/network/protocol/game/ClientboundSetPassengersPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onOnEntityPassengersSet(
        ClientboundSetPassengersPacket entityPassengersSetS2CPacket_1,
        CallbackInfo ci
    ) {
        Entity entity_1 = this.level.getEntity(entityPassengersSetS2CPacket_1.getVehicle());
        if (entity_1 == null) {
            if (!isReProcessingPassengerPacket) {
                Helper.log("Re-processed riding packet");
                IPGlobal.CLIENT_TASK_LIST.addTask(() -> {
                    isReProcessingPassengerPacket = true;
                    handleSetEntityPassengersPacket(entityPassengersSetS2CPacket_1);
                    isReProcessingPassengerPacket = false;
                    return true;
                });
                ci.cancel();
            }
        }
    }
    
    // for debug
    @Redirect(
        method = "Lnet/minecraft/client/multiplayer/ClientPacketListener;handleSetEntityData(Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private Entity redirectGetEntityById(ClientLevel clientWorld, int id) {
        Entity entity = clientWorld.getEntity(id);
        if (entity == null) {
            if (LOG_LIMIT.tryDecrement()) {
                LOGGER.warn("missing entity for data tracking {} {}", clientWorld, id);
            }
        }
        return entity;
    }
    
    // make sure that the game time is synchronized for all dimensions
    @Inject(
        method = "handleSetTime",
        at = @At("RETURN")
    )
    private void onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            ClientLevel currentWorld = Minecraft.getInstance().level;
            for (ClientLevel clientWorld : ClientWorldLoader.getClientWorlds()) {
                if (clientWorld != currentWorld) {
                    clientWorld.setGameTime(packet.getGameTime());
                }
            }
        }
    }
    
    /**
     * Vanilla has a block change acknowledge system.
     * All player actions that involve block change has a sequence number.
     * The server will send acknowledge packet to client every tick to tell that the server acknowledged the action.
     * In the client, each dimension has a {@link BlockStatePredictionHandler}.
     * When the player is performing action, it will start prediction and all client-side block changes will be enqueued with sequence number.
     * When the server send acknowledge packet, the client will apply and dequeue the block changes with sequence number smaller or equal than the acknowledgement sequence number.
     * When the server sends block update, the block state in the queue of block changes will get updated.
     * As ImmPtl has cross-dimensional block interaction, it needs to acknowledge all worlds.
     * In {@link MixinBlockStatePredictionHandler} the sequence number is kept sync across dimensions.
     */
    @Redirect(
        method = "handleBlockChangedAck",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;handleBlockChangedAck(I)V"
        )
    )
    private void redirectHandleBlockChangedAck(ClientLevel instance, int seqNumber) {
        for (ClientLevel clientWorld : ClientWorldLoader.getClientWorlds()) {
            clientWorld.handleBlockChangedAck(seqNumber);
        }
    }
    
    @Inject(
        method = "handleAddEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onHandleAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        int entityId = packet.getId();
        
        Entity existingEntity = level.getEntity(entityId);
        
        if (existingEntity != null && !existingEntity.getPassengers().isEmpty()) {
            LOGGER.warn("[ImmPtl] Entity already exists and has passengers when accepting add-entity packet. Ignoring. {} {}", existingEntity, packet);
            ci.cancel();
        }
    }
    
//    // It uses `this.level` which is not correct when switched (the lambda captures this)
//    // Make it capture level
//    @Inject(
//        method = "handleLevelChunkWithLight",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/multiplayer/ClientLevel;queueLightUpdate(Ljava/lang/Runnable;)V"
//        ),
//        cancellable = true
//    )
//    private void redirectQueueLightUpdate(
//        ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci
//    ) {
//        ClientLevel world = this.level;
//        int x = packet.getX();
//        int z = packet.getZ();
//        ClientboundLightUpdatePacketData lightData = packet.getLightData();
//        world.queueLightUpdate(() -> {
//            this.applyLightData(x, z, lightData);
//            LevelChunk levelChunk = world.getChunkSource().getChunk(x, z, false);
//            if (levelChunk != null) {
//                this.enableChunkLight(levelChunk, i, j);
//            }
//
//        });
//        ci.cancel();
//
//    }
    
    // for debugging
    @Inject(
        method = "handleLevelChunkWithLight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onHandleLevelChunkWithLight(
        ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci
    ) {
        if (IPGlobal.chunkPacketDebug) {
            LOGGER.info("Chunk Load Packet {} {} {}", level.dimension().location(), packet.getX(), packet.getZ());
        }
    }
    
    // for debugging
    @Inject(
        method = "handleForgetLevelChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onHandleForgetLevelChunk(
        ClientboundForgetLevelChunkPacket packet, CallbackInfo ci
    ) {
        if (IPGlobal.chunkPacketDebug) {
            LOGGER.info(
                "Chunk Unload Packet {} {} {}",
                level.dimension().location(), packet.pos().x, packet.pos().z
            );
        }
    }
}
