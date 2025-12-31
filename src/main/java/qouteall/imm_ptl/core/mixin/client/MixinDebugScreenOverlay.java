package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenOverlay {
    @Shadow
    private void renderLines(GuiGraphics guiGraphics, List<String> lines, boolean left) {
        throw new IllegalStateException("Mixin failed to shadow DebugScreenOverlay.renderLines");
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;renderLines(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;Z)V"
        )
    )
    private void redirectRenderLines(
        DebugScreenOverlay instance,
        GuiGraphics guiGraphics,
        List<String> lines,
        boolean left
    ) {
        if (!left) {
            List<String> debugText = RenderStates.collectDebugText();
            if (IPGlobal.moveDebugTextToTop) {
                lines.addAll(0, debugText);
            }
            else {
                lines.addAll(debugText);
            }
        }

        renderLines(guiGraphics, lines, left);
    }
    
}
