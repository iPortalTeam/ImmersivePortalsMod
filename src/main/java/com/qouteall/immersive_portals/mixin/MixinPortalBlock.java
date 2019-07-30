package com.qouteall.immersive_portals.mixin;

import net.minecraft.block.PortalBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalBlock.class)
public class MixinPortalBlock {
    
    @Inject(
        method = "Lnet/minecraft/block/PortalBlock;createPortalAt(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCreatePortal(
        IWorld iWorld_1,
        BlockPos blockPos_1,
        CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
        cir.cancel();
    }
}
