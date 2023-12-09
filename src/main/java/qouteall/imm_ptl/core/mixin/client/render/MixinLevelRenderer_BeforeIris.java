package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;

@Mixin(value = LevelRenderer.class, priority = 900)
public class MixinLevelRenderer_BeforeIris {
    // inject it after Iris, run before Iris
    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=translucent"))
    private void iris$beginTranslucents(
        PoseStack poseStack, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightTexture lightTexture, Matrix4f projection,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.onBeginIrisTranslucentRendering(poseStack);
    }
}
