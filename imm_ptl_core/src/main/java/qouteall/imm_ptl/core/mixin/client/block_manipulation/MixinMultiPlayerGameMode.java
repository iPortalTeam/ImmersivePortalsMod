package qouteall.imm_ptl.core.mixin.client.block_manipulation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.ducks.IEClientPlayerInteractionManager;
import qouteall.imm_ptl.core.network.IPNetworkingClient;
import qouteall.q_misc_util.Helper;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IEClientPlayerInteractionManager {
    @Shadow
    @Final
    private ClientPacketListener connection;
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @ModifyArg(
        method = "startPrediction",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private Packet modifyPacketInStartPrediction(Packet<?> packet) {
        if (BlockManipulationClient.isContextSwitched) {
            if (packet instanceof ServerboundPlayerActionPacket playerActionPacket) {
                return IPNetworkingClient.createCtsPlayerAction(
                    BlockManipulationClient.remotePointedDim, playerActionPacket
                );
            }
            else if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket) {
                return IPNetworkingClient.createCtsRightClick(
                    BlockManipulationClient.remotePointedDim, useItemOnPacket
                );
            }
            else {
                // TODO ServerboundUseItemPacket
                Helper.err("Unknown packet in startPrediction");
                return packet;
            }
        }
        else {
            return packet;
        }
    }
    
    @ModifyArg(
        method = "startDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private Packet redirectSendInStartDestroyBlock(Packet packet) {
        if (BlockManipulationClient.isContextSwitched) {
            return IPNetworkingClient.createCtsPlayerAction(
                BlockManipulationClient.remotePointedDim, (ServerboundPlayerActionPacket) packet
            );
        }
        else {
            return packet;
        }
    }
    
    @ModifyArg(
        method = "stopDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private Packet redirectSendInStopDestroyBlock(Packet packet) {
        if (BlockManipulationClient.isContextSwitched) {
            return IPNetworkingClient.createCtsPlayerAction(
                BlockManipulationClient.remotePointedDim, (ServerboundPlayerActionPacket) packet
            );
        }
        else {
            return packet;
        }
    }
    
    // TODO should inject releaseUsingItem?
    
    
}
