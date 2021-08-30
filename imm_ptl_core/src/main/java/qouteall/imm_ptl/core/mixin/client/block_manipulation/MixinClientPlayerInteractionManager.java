package qouteall.imm_ptl.core.mixin.client.block_manipulation;

import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;
import qouteall.imm_ptl.core.ducks.IEClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements IEClientPlayerInteractionManager {
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
                IPNetworkingClient.createCtsPlayerAction(
                    BlockManipulationClient.remotePointedDim,
                    new PlayerActionC2SPacket(action, blockPos, direction)
                )
            );
            ci.cancel();
        }
    }
    
    @ModifyArg(
        method = "interactBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private Packet<?> redirectSendPacketOnInteractBlock(
        Packet<?> packet
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            return IPNetworkingClient.createCtsRightClick(
                BlockManipulationClient.remotePointedDim,
                ((PlayerInteractBlockC2SPacket) packet)
            );
        }
        else {
            return packet;
        }
    }
    
}
