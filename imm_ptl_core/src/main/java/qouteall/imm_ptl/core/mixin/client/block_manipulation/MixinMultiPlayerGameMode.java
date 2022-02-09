package qouteall.imm_ptl.core.mixin.client.block_manipulation;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.ducks.IEClientPlayerInteractionManager;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IEClientPlayerInteractionManager {
    @Shadow
    @Final
    private ClientPacketListener connection;
    
    @Shadow
    @Final
    private Object2ObjectLinkedOpenHashMap<Pair<BlockPos, ServerboundPlayerActionPacket.Action>, Vec3> unAckedActions;
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    // vanilla copy
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;sendBlockAction(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendPlayerAction(
        ServerboundPlayerActionPacket.Action action,
        BlockPos blockPos,
        Direction direction,
        CallbackInfo ci
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            this.unAckedActions.put(Pair.of(blockPos, action), minecraft.player.position());
            this.connection.send(
                IPNetworkingClient.createCtsPlayerAction(
                    BlockManipulationClient.remotePointedDim,
                    new ServerboundPlayerActionPacket(action, blockPos, direction)
                )
            );
            ci.cancel();
        }
    }
    
    @ModifyArg(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
        )
    )
    private Packet<?> redirectSendPacketOnInteractBlock(
        Packet<?> packet
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            return IPNetworkingClient.createCtsRightClick(
                BlockManipulationClient.remotePointedDim,
                ((ServerboundUseItemOnPacket) packet)
            );
        }
        else {
            return packet;
        }
    }
    
}
