package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderRegionCache.class)
public class MixinChunkRendererRegionBuilder {
    //will this avoid that random crash?
    @Inject(
        method = "Lnet/minecraft/client/renderer/chunk/RenderRegionCache;createRegion(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBuild(
        Level worldIn,
        BlockPos from,
        BlockPos to,
        int padding,
        CallbackInfoReturnable<RenderChunkRegion> cir
    ) {
        if (worldIn == null) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
    
    
}
