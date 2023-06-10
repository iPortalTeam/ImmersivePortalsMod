package qouteall.imm_ptl.peripheral.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.peripheral.ImmPtlCustomOverlay;

@Mixin(Gui.class)
public class MixinGui {
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Inject(
        method = "render", at = @At("RETURN")
    )
    private void onRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (!this.minecraft.options.hideGui) {
            ImmPtlCustomOverlay.render(guiGraphics, partialTick);
        }
    }
}
