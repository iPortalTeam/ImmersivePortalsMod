package com.qouteall.immersive_portals.mixin_client.far_scenery;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_F {
//    //for non-optifine
//    @Inject(
//        method = "getAdjacentChunk",
//        at = @At("RETURN"),
//        cancellable = true,
//        require = 0
//    )
//    private void onGetAdjacentChunkReturn(
//        BlockPos pos,
//        ChunkBuilder.BuiltChunk chunk,
//        Direction direction,
//        CallbackInfoReturnable<ChunkBuilder.BuiltChunk> cir
//    ) {
//        if (FarSceneryRenderer.isRenderingScenery) {
//            ChunkBuilder.BuiltChunk builtChunk = cir.getReturnValue();
//            boolean shouldRenderInNearScenery = FaceRenderingTask.shouldRenderInNearScenery(
//                builtChunk,
//                MinecraftClient.getInstance().gameRenderer.getCamera().getPos(),
//                FarSceneryRenderer.currDistance
//            );
//            if (!shouldRenderInNearScenery) {
//                cir.setReturnValue(null);
//            }
//        }
//    }
//
//    //for optifine
//    @Inject(
//        method = "Lnet/minecraft/client/render/WorldRenderer;getRenderChunkOffset(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;Lnet/minecraft/util/math/Direction;ZI)Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;",
//        at = @At("RETURN"),
//        cancellable = true,
//        require = 1
//    )
//    private void onGetRenderChunkOffset(
//        BlockPos playerPos,
//        ChunkBuilder.BuiltChunk renderChunkBase,
//        Direction facing,
//        boolean fog,
//        int yMax,
//        CallbackInfoReturnable<ChunkBuilder.BuiltChunk> cir
//    ) {
//        if (FarSceneryRenderer.isRenderingScenery) {
//            ChunkBuilder.BuiltChunk builtChunk = cir.getReturnValue();
//            boolean shouldRenderInNearScenery = FaceRenderingTask.shouldRenderInNearScenery(
//                builtChunk,
//                MinecraftClient.getInstance().gameRenderer.getCamera().getPos(),
//                FarSceneryRenderer.currDistance
//            );
//            if (!shouldRenderInNearScenery) {
//                cir.setReturnValue(null);
//            }
//        }
//    }
}
