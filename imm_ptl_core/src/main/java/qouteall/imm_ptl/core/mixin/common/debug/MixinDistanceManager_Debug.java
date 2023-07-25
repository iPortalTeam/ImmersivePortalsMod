package qouteall.imm_ptl.core.mixin.common.debug;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.debug.DebugUtil;

@Mixin(DistanceManager.class)
public class MixinDistanceManager_Debug {
    @Shadow
    @Final
    private LongSet ticketsToRelease;
    
    // debug
    @Inject(
        method = "runAllUpdates",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/LongSet;isEmpty()Z"
        )
    )
    private void onRunAllUpdates2(ChunkMap chunkManager, CallbackInfoReturnable<Boolean> cir) {
        DebugUtil.releaseCounter += ticketsToRelease.size();
    }
}
