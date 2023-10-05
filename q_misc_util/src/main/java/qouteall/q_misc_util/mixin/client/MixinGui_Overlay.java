package qouteall.q_misc_util.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.CustomTextOverlay;

@Mixin(Gui.class)
public class MixinGui_Overlay {
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Inject(
        method = "render", at = @At("RETURN")
    )
    private void onRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (!this.minecraft.options.hideGui) {
            CustomTextOverlay.render(guiGraphics, partialTick);
        }
    }
}
