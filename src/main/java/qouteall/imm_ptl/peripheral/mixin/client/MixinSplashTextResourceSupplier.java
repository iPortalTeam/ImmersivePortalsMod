package qouteall.imm_ptl.peripheral.mixin.client;

import net.minecraft.client.resources.SplashManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SplashManager.class)
public class MixinSplashTextResourceSupplier {
    @Shadow
    @Final
    private List<String> splashes;
    
    @Inject(method = "Lnet/minecraft/client/resources/SplashManager;apply(Ljava/lang/Object;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("RETURN"))
    private void onApply(
        List<String> list, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci
    ) {
        splashes.remove("Euclidian!");
        splashes.remove("Slow acting portals!");
        splashes.add("Non-Euclidean!");
        splashes.add("Fast acting portals!");
        splashes.add("Immersive Portals!");
    }
}
