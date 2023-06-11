package qouteall.imm_ptl.core.mixin.common.interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;

@Mixin(ServerPlayerGameMode.class)
public class MixinServerPlayerGameMode {
    @Shadow
    protected ServerLevel level;
    
    @Shadow
    @Final
    protected ServerPlayer player;
    
    @Shadow
    private boolean hasDelayedDestroy;
    @Shadow
    private boolean isDestroyingBlock;
    
    private ServerLevel ip_destroyPosLevel;
    
    private ServerLevel ip_getActualWorld() {
        ServerLevel redirect = BlockManipulationServer.SERVER_PLAYER_INTERACTION_REDIRECT.get();
        if (redirect != null) {
            return redirect;
        }
        return level;
    }
    
    // use the actual dimension
    @Redirect(
        method = {
            "incrementDestroyProgress",
            "handleBlockBreakAction",
            "destroyAndAck",
            "destroyBlock",
        },
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;level:Lnet/minecraft/server/level/ServerLevel;"
        )
    )
    private ServerLevel redirectGetLevel(
        ServerPlayerGameMode serverPlayerGameMode
    ) {
        return ip_getActualWorld();
    }
    
    // use the actual dimension
    @Redirect(
        method = {
            "incrementDestroyProgress",
            "handleBlockBreakAction"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;level()Lnet/minecraft/world/level/Level;"
        )
    )
    private Level redirectGetLevel(ServerPlayer instance) {
        return ip_getActualWorld();
    }
    
    // use the actual dimension
    @Redirect(
        method = "useItemOn",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/item/context/UseOnContext;"
        )
    )
    private UseOnContext redirectNewUseOnContext(Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return new UseOnContext(
            ip_getActualWorld(), player, interactionHand,
            player.getItemInHand(interactionHand), blockHitResult
        );
    }
    
    // disable distance check when doing cross-portal interaction
    @WrapOperation(
        method = "handleBlockBreakAction",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
        )
    )
    private double redirectDistanceInHandleBlockBreakAction(Vec3 instance, Vec3 vec, Operation<Double> original) {
        ServerLevel redirect = BlockManipulationServer.SERVER_PLAYER_INTERACTION_REDIRECT.get();
        if (redirect != null) {
            return 0;
        }
        return original.call(instance, vec);
    }
    
    // record the world for the destroying pos
    @Inject(
        method = "handleBlockBreakAction",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyPos:Lnet/minecraft/core/BlockPos;"
        )
    )
    private void onSetDestroyPos(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face, int maxBuildHeight, int sequence, CallbackInfo ci) {
        ip_destroyPosLevel = ip_getActualWorld();
    }
    
    // record the world for the delayed destroy pos
    @Inject(
        method = "handleBlockBreakAction",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;delayedDestroyPos:Lnet/minecraft/core/BlockPos;"
        )
    )
    private void onSetDelayedDestroyPos(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face, int maxBuildHeight, int sequence, CallbackInfo ci) {
        ip_destroyPosLevel = ip_getActualWorld();
    }
    
    // use the recorded destroy pos world
    @Redirect(
        method = "tick",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;level:Lnet/minecraft/server/level/ServerLevel;"
        )
    )
    private ServerLevel redirectLevelOnTicking(ServerPlayerGameMode instance) {
        if (ip_destroyPosLevel != null) {
            return ip_destroyPosLevel;
        }
        return level;
    }
    
    @Inject(
        method = "tick", at = @At("RETURN")
    )
    private void onTickingEnd(CallbackInfo ci) {
        if (!hasDelayedDestroy && !isDestroyingBlock) {
            ip_destroyPosLevel = null;
        }
    }
}
