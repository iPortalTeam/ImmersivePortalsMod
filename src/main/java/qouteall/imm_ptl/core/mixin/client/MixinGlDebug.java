package qouteall.imm_ptl.core.mixin.client;

import com.mojang.blaze3d.platform.GlDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// set glDebugVerbosity:3 in options.txt
@Mixin(GlDebug.class)
public class MixinGlDebug {
    private static int loggedNum = 0;
    
    @Inject(
        method = "Lcom/mojang/blaze3d/platform/GlDebug;printDebugLog(IIIIIJJ)V", at = @At("RETURN")
    )
    private static void onLogging(
        int source, int type, int id, int severity, int messageLength, long message, long l,
        CallbackInfo ci
    ) {
        if (loggedNum < 100) {
            new Throwable().printStackTrace();
            loggedNum++;
        }
    }
}
