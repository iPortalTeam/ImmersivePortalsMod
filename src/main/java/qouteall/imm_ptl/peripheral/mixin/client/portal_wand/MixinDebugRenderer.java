package qouteall.imm_ptl.peripheral.mixin.client.portal_wand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.peripheral.wand.ClientPortalWandInteraction;

@Mixin(DebugRenderer.class)
public class MixinDebugRenderer {
    // let's put portal wand marking render into debug renderer
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void onRender(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ,
        CallbackInfo ci
    ) {
        ClientPortalWandInteraction.render(poseStack, bufferSource, camX, camY, camZ);
    }
}
