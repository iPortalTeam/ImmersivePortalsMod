package qouteall.imm_ptl.peripheral.mixin.client;

import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SplashTextResourceSupplier.class)
public class MixinSplashTextResourceSupplier {
    @Shadow
    @Final
    private List<String> splashTexts;
    
    @Inject(method = "apply", at = @At("RETURN"))
    private void onApply(
        List<String> list, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci
    ) {
        splashTexts.remove("Euclidian!");
        splashTexts.remove("Slow acting portals!");
        splashTexts.add("Non-Euclidean!");
        splashTexts.add("Fast acting portals!");
        splashTexts.add("Immersive Portals!");
    }
}
