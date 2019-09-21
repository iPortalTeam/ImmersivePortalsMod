package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.exposer.IECamera;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.render.RenderHelper;
import net.minecraft.block.Block;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class MixinCamera implements IECamera {
    double lastClipSpaceResult = 1;
    
    @Shadow
    private net.minecraft.util.math.Vec3d pos;
    @Shadow
    private BlockView area;
    @Shadow
    private Entity focusedEntity;
    @Shadow
    private net.minecraft.util.math.Vec3d horizontalPlane;
    
    @Inject(
        method = "getSubmergedFluidState",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSubmergedFluidState(CallbackInfoReturnable<FluidState> cir) {
        if (CGlobal.renderer.isRendering()) {
            cir.setReturnValue(Fluids.EMPTY.getDefaultState());
            cir.cancel();
        }
    }
    
    @Shadow
    protected abstract void setPos(net.minecraft.util.math.Vec3d vec3d_1);
    
    @Override
    public void setPos_(Vec3d pos) {
        setPos(pos);
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    private double clipToSpace(double upperBound) {
        if (CGlobal.renderer.isRendering()) {
            return lastClipSpaceResult;
        }
        
        for (int int_1 = 0; int_1 < 8; ++int_1) {
            float dx = (float) ((int_1 & 1) * 2 - 1);
            float dy = (float) ((int_1 >> 1 & 1) * 2 - 1);
            float dz = (float) ((int_1 >> 2 & 1) * 2 - 1);
            dx *= 0.1F;
            dy *= 0.1F;
            dz *= 0.1F;
            net.minecraft.util.math.Vec3d origin = this.pos.add(
                (double) dx,
                (double) dy,
                (double) dz
            );
            net.minecraft.util.math.Vec3d dest = new net.minecraft.util.math.Vec3d(
                this.pos.x - this.horizontalPlane.x * upperBound + (double) dx + (double) dz,
                this.pos.y - this.horizontalPlane.y * upperBound + (double) dy,
                this.pos.z - this.horizontalPlane.z * upperBound + (double) dz
            );
            BlockHitResult hitResult1 = this.area.rayTrace(new RayTraceContext(
                origin,
                dest,
                RayTraceContext.ShapeType.COLLIDER,
                RayTraceContext.FluidHandling.NONE,
                this.focusedEntity
            ));
            if (hitResult1.getType() != HitResult.Type.MISS) {
                double double_2 = hitResult1.getPos().distanceTo(this.pos);
                if (double_2 < upperBound) {
                    upperBound = double_2;
                }
                continue;
            }
            BlockHitResult hitResult2 = this.area.rayTrace(new RayTraceContext(
                origin,
                dest,
                RayTraceContext.ShapeType.OUTLINE,
                RayTraceContext.FluidHandling.NONE,
                this.focusedEntity
            ));
            if (hitResult2.getType() != HitResult.Type.MISS) {
                Block hittedBlock = area.getBlockState(hitResult2.getBlockPos()).getBlock();
                if (hittedBlock == PortalPlaceholderBlock.instance) {
                    double double_2 = hitResult2.getPos().distanceTo(this.pos);
                    if (double_2 < upperBound) {
                        upperBound = double_2;
                    }
                }
            }
        }
        
        lastClipSpaceResult = upperBound;
        
        return upperBound;
    }
    
    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdated(
        BlockView p_216772_1_,
        Entity p_216772_2_,
        boolean p_216772_3_,
        boolean p_216772_4_,
        float p_216772_5_,
        CallbackInfo ci
    ) {
        RenderHelper.setupTransformationForMirror((Camera) (Object) this);
    }
}
