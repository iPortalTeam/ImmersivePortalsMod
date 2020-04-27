package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class MixinDebugHud {
    @Inject(method = "getRightText", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> returnValue = cir.getReturnValue();
        returnValue.add("Rendered Portal Num: " + MyRenderHelper.lastPortalRenderInfos.size());
//        ClientWorld world = MinecraftClient.getInstance().world;
//        if (world != null) {
//            returnValue.add("In: " + world.dimension.getType());
//        }
        if (MyRenderHelper.debugText != null && !MyRenderHelper.debugText.isEmpty()) {
            returnValue.add("Debug: " + MyRenderHelper.debugText);
        }
    }
}
