package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IECamera;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class MixinCamera implements IECamera {
    private double lastClipSpaceResult = 1;
    
    @Shadow
    private net.minecraft.util.math.Vec3d pos;
    @Shadow
    private BlockView area;
    @Shadow
    private Entity focusedEntity;
    
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
    
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpaceHead(double double_1, CallbackInfoReturnable<Double> cir) {
        if (CGlobal.renderer.isRendering()) {
            cir.setReturnValue(lastClipSpaceResult);
            cir.cancel();
        }
    }
    
    @Inject(method = "clipToSpace", at = @At("RETURN"), cancellable = true)
    private void onClipToSpaceReturn(double double_1, CallbackInfoReturnable<Double> cir) {
        lastClipSpaceResult = cir.getReturnValue();
    }
    
}
