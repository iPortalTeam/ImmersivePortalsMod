package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.PortalUtils;

@Mixin(Container.class)
public interface MixinContainer {
    @Inject(
        method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;I)Z",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onStillValidBlockEntity(
        BlockEntity blockEntity, Player player, int distance, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            PortalUtils.PortalAwareRaytraceResult result = PortalUtils.portalAwareRayTrace(
                player.level(),
                player.getEyePosition(),
                player.getViewVector(1),
                32,
                player,
                ClipContext.Block.COLLIDER
            );
            if (result != null &&
                result.hitResult().getBlockPos().equals(blockEntity.getBlockPos())
            ) {
                cir.setReturnValue(true);
            }
        }
    }
}
