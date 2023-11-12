package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.imm_ptl.core.render.ForceMainThreadRebuild;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_ForceMainThreadRebuild {
    @ModifyVariable(
        method = "compileSections",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Options;prioritizeChunkUpdates()Lnet/minecraft/client/OptionInstance;",
            ordinal = 0
        )
    )
    private boolean modifyShouldImmediatelyRebuild(boolean originalValue) {
        if (ForceMainThreadRebuild.isCurrentFrameForceMainThreadRebuild()) {
            return true;
        }
        else {
            return originalValue;
        }
    }
}
