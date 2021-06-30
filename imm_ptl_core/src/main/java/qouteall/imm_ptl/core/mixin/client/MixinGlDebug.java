package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.client.gl.GlDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class MixinGlDebug {
    private static int loggedNum = 0;
    
    @Inject(
        method = "info", at = @At("RETURN")
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
