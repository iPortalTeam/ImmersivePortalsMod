package qouteall.imm_ptl.core.mixin.common.debug;

import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DistanceManager.class)
public class MixinDistanceManager_Debug {
//    @Shadow
//    @Final
//    private LongSet ticketsToRelease;
//
//    // debug
//    @Inject(
//        method = "runAllUpdates",
//        at = @At(
//            value = "INVOKE",
//            target = "Lit/unimi/dsi/fastutil/longs/LongSet;isEmpty()Z"
//        )
//    )
//    private void onRunAllUpdates2(ChunkMap chunkManager, CallbackInfoReturnable<Boolean> cir) {
//        DebugUtil.releaseCounter += ticketsToRelease.size();
//    }
}
