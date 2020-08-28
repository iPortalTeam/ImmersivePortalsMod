package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRendererRegion.class)
public class MixinChunkRendererRegion {
    //will this avoid that random crash?
    @Inject(
        method = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;create(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;I)Lnet/minecraft/client/render/chunk/ChunkRendererRegion;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onCreate(
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
