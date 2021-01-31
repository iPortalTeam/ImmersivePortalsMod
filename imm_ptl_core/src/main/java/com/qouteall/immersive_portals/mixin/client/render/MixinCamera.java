package com.qouteall.immersive_portals.mixin.client.render;

import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class MixinCamera implements IECamera {
    private static double lastClipSpaceResult = 1;
    
    @Shadow
    private Vec3d pos;
    @Shadow
    private BlockView area;
    @Shadow
    private Entity focusedEntity;
    @Shadow
    private float cameraY;
    @Shadow
    private float lastCameraY;
    
    @Shadow
    protected abstract void setPos(Vec3d vec3d_1);
    
    @Inject(
        method = "update",
        at = @At("RETURN")
    )
    private void onUpdateFinished(
        BlockView area, Entity focusedEntity, boolean thirdPerson,
        boolean inverseView, float tickDelta, CallbackInfo ci
    ) {
        Camera this_ = (Camera) (Object) this;
        WorldRenderInfo.adjustCameraPos(this_);
    }
    
    @Inject(
        method = "getSubmergedFluidState",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSubmergedFluidState(CallbackInfoReturnable<FluidState> cir) {
        if (PortalRendering.isRendering()) {
            cir.setReturnValue(Fluids.EMPTY.getDefaultState());
            cir.cancel();
        }
    }
    
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpaceHead(double double_1, CallbackInfoReturnable<Double> cir) {
        if (PortalRendering.isRendering()) {
            cir.setReturnValue(lastClipSpaceResult);
            cir.cancel();
        }
    }
    
    @Inject(method = "clipToSpace", at = @At("RETURN"), cancellable = true)
    private void onClipToSpaceReturn(double double_1, CallbackInfoReturnable<Double> cir) {
        lastClipSpaceResult = cir.getReturnValue();
    }
    
    //to let the player be rendered when rendering portal
    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void onIsThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (CrossPortalEntityRenderer.shouldRenderPlayerItself()) {
            cir.setReturnValue(true);
        }
    }
    
    @Override
    public void resetState(Vec3d pos, ClientWorld currWorld) {
        setPos(pos);
        area = currWorld;
    }
    
    @Override
    public float getCameraY() {
        return cameraY;
    }
    
    @Override
    public float getLastCameraY() {
        return lastCameraY;
    }
    
    @Override
    public void setCameraY(float cameraY_, float lastCameraY_) {
        cameraY = cameraY_;
        lastCameraY = lastCameraY_;
    }
    
    @Override
    public void portal_setPos(Vec3d pos) {
        setPos(pos);
    }
    
    @Override
    public void portal_setFocusedEntity(Entity arg) {
        focusedEntity = arg;
    }
}
