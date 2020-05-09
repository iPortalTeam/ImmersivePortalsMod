package com.qouteall.immersive_portals.mixin_client.block_manipulation;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import com.qouteall.immersive_portals.ducks.IEClientPlayerInteractionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements IEClientPlayerInteractionManager {
    @Shadow
    @Final
    private MinecraftClient client;
    
    @Shadow
    @Final
    private ClientPlayNetworkHandler networkHandler;
    
    @Inject(
        method = "sendPlayerAction",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendPlayerAction(
        PlayerActionC2SPacket.Action action,
        BlockPos blockPos,
        Direction direction,
        CallbackInfo ci
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            this.networkHandler.sendPacket(
                MyNetworkClient.createCtsPlayerAction(
                    BlockManipulationClient.remotePointedDim,
                    new PlayerActionC2SPacket(action, blockPos, direction)
                )
            );
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "interactBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void redirectSendPacketOnInteractBlock(
        ClientPlayNetworkHandler clientPlayNetworkHandler,
        Packet<?> packet
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            clientPlayNetworkHandler.sendPacket(
                MyNetworkClient.createCtsRightClick(
                    BlockManipulationClient.remotePointedDim,
                    ((PlayerInteractBlockC2SPacket) packet)
                )
            );
        }
        else {
            clientPlayNetworkHandler.sendPacket(packet);
        }
    }
    
    @Inject(
        method = "hasExtendedReach",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHasExtendedReach(CallbackInfoReturnable<Boolean> cir) {
        if (Global.longerReachInCreative) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
    
    @Inject(
        method = "getReachDistance",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void onGetReachDistance(CallbackInfoReturnable<Float> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double result = cir.getReturnValue() *
            HandReachTweak.getActualHandReachMultiplier(player);
        cir.setReturnValue((float) result);
        cir.cancel();
    }
    
}
