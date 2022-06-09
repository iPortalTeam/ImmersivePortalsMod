package qouteall.imm_ptl.core.mixin.client.block_manipulation;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.ducks.IEClientPlayerInteractionManager;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;
import qouteall.q_misc_util.Helper;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IEClientPlayerInteractionManager {
    @Shadow
    @Final
    private ClientPacketListener connection;
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Redirect(
        method = "startPrediction",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void onStartPrediction(ClientPacketListener instance, Packet<?> packet) {
        if (BlockManipulationClient.isContextSwitched) {
            if (packet instanceof ServerboundPlayerActionPacket playerActionPacket) {
                instance.send(IPNetworkingClient.createCtsPlayerAction(
                    BlockManipulationClient.remotePointedDim, playerActionPacket
                ));
            }
            else if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket) {
                instance.send(IPNetworkingClient.createCtsRightClick(
                    BlockManipulationClient.remotePointedDim, useItemOnPacket
                ));
            }
            else {
                Helper.err("Unknown packet in startPrediction");
                instance.send(packet);
            }
        }
        else {
            instance.send(packet);
        }
    }
    
    @Redirect(
        method = "startDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void redirectSendInStartDestroyBlock(ClientPacketListener instance, Packet packet) {
        if (BlockManipulationClient.isContextSwitched) {
            instance.send(IPNetworkingClient.createCtsPlayerAction(
                BlockManipulationClient.remotePointedDim, (ServerboundPlayerActionPacket) packet
            ));
        }
        else {
            instance.send(packet);
        }
    }
    
    @Redirect(
        method = "stopDestroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private void redirectSendInStopDestroyBlock(ClientPacketListener instance, Packet packet) {
        if (BlockManipulationClient.isContextSwitched) {
            instance.send(IPNetworkingClient.createCtsPlayerAction(
                BlockManipulationClient.remotePointedDim, (ServerboundPlayerActionPacket) packet
            ));
        }
        else {
            instance.send(packet);
        }
    }
    
    // TODO should inject releaseUsingItem?
    
    
    
}
