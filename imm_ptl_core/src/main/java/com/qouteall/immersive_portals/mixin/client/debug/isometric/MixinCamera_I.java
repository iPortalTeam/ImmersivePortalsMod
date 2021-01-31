package com.qouteall.immersive_portals.mixin.client.debug.isometric;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Camera.class)
public abstract class MixinCamera_I {
//    @Shadow
//    protected abstract void moveBy(double x, double y, double z);
//
//    @Inject(method = "update", at = @At("RETURN"))
//    private void onUpdated(
//        BlockView area, Entity focusedEntity,
//        boolean thirdPerson, boolean inverseView, float tickDelta,
//        CallbackInfo ci
//    ) {
//        if (TransformationManager.isIsometricView) {
//            // isometric is equivalent to camera position in an infinitely far place
//            // however if the camera is in unloaded chunks it won't render anything
//
//            int viewDistance = MinecraftClient.getInstance().options.viewDistance;
//
//            float dist = ((viewDistance - 1) * 16) / 2.0f;
//
//            moveBy(-dist, 0, 0);
//        }
//    }
}
