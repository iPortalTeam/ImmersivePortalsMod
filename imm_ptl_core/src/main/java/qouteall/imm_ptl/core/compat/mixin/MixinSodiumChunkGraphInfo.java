package qouteall.imm_ptl.core.compat.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphInfo;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;

@Mixin(value = ChunkGraphInfo.class, remap = false)
public abstract class MixinSodiumChunkGraphInfo {
    @Shadow
    public abstract int getOriginX();
    
    @Shadow
    public abstract int getOriginY();
    
    @Shadow
    public abstract int getOriginZ();
    
    // do portal frustum culling
    @Inject(
        method = "isCulledByFrustum",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsCulledByFrustum(Frustum frustum, CallbackInfoReturnable<Boolean> cir) {
        if (SodiumInterface.frustumCuller != null) {
            double x = this.getOriginX();
            double y = this.getOriginY();
            double z = this.getOriginZ();
            
            boolean invisible = SodiumInterface.frustumCuller.canDetermineInvisibleWithWorldCoord(
                x, y, z, x + 16.0F, y + 16.0F, z + 16.0F
            );
            if (invisible) {
                cir.setReturnValue(true);
            }
        }
    }
}
