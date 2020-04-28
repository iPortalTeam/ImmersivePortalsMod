package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RayTraceContext.class)
public abstract class MixinRayTraceContext {
    @Inject(
        at = @At("HEAD"),
        method = "getBlockShape",
        cancellable = true
    )
    private void onGetBlockShape(BlockState blockState, BlockView blockView, BlockPos blockPos, CallbackInfoReturnable<VoxelShape> cir) {
        if (Global.portalPlaceholderPassthrough && blockState.getBlock() == PortalPlaceholderBlock.instance) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
