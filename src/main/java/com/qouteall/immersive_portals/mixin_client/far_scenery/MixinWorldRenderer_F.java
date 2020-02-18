package com.qouteall.immersive_portals.mixin_client.far_scenery;

import com.qouteall.immersive_portals.far_scenery.FaceRenderingTask;
import com.qouteall.immersive_portals.far_scenery.FarSceneryRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_F {
    //for non-optifine
    @Inject(
        method = "getAdjacentChunk",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void onGetAdjacentChunkReturn(
        BlockPos pos,
        ChunkBuilder.BuiltChunk chunk,
        Direction direction,
        CallbackInfoReturnable<ChunkBuilder.BuiltChunk> cir
    ) {
        if (FarSceneryRenderer.isRenderingScenery) {
            ChunkBuilder.BuiltChunk builtChunk = cir.getReturnValue();
            boolean shouldRenderInNearScenery = FaceRenderingTask.shouldRenderInNearScenery(
                builtChunk,
                MinecraftClient.getInstance().gameRenderer.getCamera().getPos(),
                FarSceneryRenderer.currDistance
            );
            if (!shouldRenderInNearScenery) {
                cir.setReturnValue(null);
            }
        }
    }
    
    //for optifine
    @Inject(
        method = "Lnet/minecraft/client/render/WorldRenderer;getRenderChunkOffset(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;Lnet/minecraft/util/math/Direction;ZI)Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void onGetRenderChunkOffset(
        BlockPos playerPos,
        ChunkBuilder.BuiltChunk renderChunkBase,
        Direction facing,
        boolean fog,
        int yMax,
        CallbackInfoReturnable<ChunkBuilder.BuiltChunk> cir
    ) {
        if (FarSceneryRenderer.isRenderingScenery) {
            ChunkBuilder.BuiltChunk builtChunk = cir.getReturnValue();
            boolean shouldRenderInNearScenery = FaceRenderingTask.shouldRenderInNearScenery(
                builtChunk,
                MinecraftClient.getInstance().gameRenderer.getCamera().getPos(),
                FarSceneryRenderer.currDistance
            );
            if (!shouldRenderInNearScenery) {
                cir.setReturnValue(null);
            }
        }
    }
}
