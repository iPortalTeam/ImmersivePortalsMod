package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRendererRegionBuilder.class)
public class MixinChunkRendererRegionBuilder {
    //will this avoid that random crash?
    @Inject(
        method = "Lnet/minecraft/client/render/chunk/ChunkRendererRegionBuilder;build(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;I)Lnet/minecraft/client/render/chunk/ChunkRendererRegion;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBuild(
        World worldIn,
        BlockPos from,
        BlockPos to,
        int padding,
        CallbackInfoReturnable<ChunkRendererRegion> cir
    ) {
        if (worldIn == null) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
    
    
}
