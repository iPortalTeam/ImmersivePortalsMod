package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IERayTraceContext;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RayTraceContext.class)
public abstract class MixinRayTraceContext implements IERayTraceContext {
    @SuppressWarnings("ShadowModifiers")
    @Shadow
    private Vec3d start;
    
    @SuppressWarnings("ShadowModifiers")
    @Shadow
    private Vec3d end;
    
    @Override
    public IERayTraceContext setStart(Vec3d newStart) {
        start = newStart;
        return this;
    }
    
    @Override
    public IERayTraceContext setEnd(Vec3d newEnd) {
        end = newEnd;
        return this;
    }
    
    @Inject(
        at = @At("HEAD"),
        method = "getBlockShape",
        cancellable = true
    )
    private void onGetBlockShape(
        BlockState blockState,
        BlockView blockView,
        BlockPos blockPos,
        CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (Global.portalPlaceholderPassthrough && blockState.getBlock() == PortalPlaceholderBlock.instance) {
            if (blockView instanceof World) {
                boolean isIntersectingWithPortal = McHelper.getEntitiesRegardingLargeEntities(
                    (World) blockView, new Box(blockPos),
                    10, Portal.class, e -> true
                ).isEmpty();
                if (!isIntersectingWithPortal) {
                    cir.setReturnValue(VoxelShapes.empty());
                }
            }
        }
    }
}
