package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onBeginRender(
        BlockEntity blockEntity_1,
        float float_1,
        int int_1,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            Portal renderingPortal = CGlobal.renderer.getRenderingPortal();
            if (!renderingPortal.canRenderEntityInsideMe(new Vec3d(blockEntity_1.getPos()))) {
                ci.cancel();
            }
        }
    }
}
