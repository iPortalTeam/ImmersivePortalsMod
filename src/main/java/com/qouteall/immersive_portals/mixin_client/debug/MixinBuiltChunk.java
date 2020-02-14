package com.qouteall.immersive_portals.mixin_client.debug;

import com.qouteall.immersive_portals.CHelper;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class MixinBuiltChunk {
    @Shadow
    private ChunkBuilder.BuiltChunk[] renderChunkNeighbours;
    
    @Inject(
        method = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;updateRenderChunkNeighboursValid()V",
        at = @At("HEAD")
    )
    private void what(CallbackInfo ci) {
        int north = Direction.NORTH.ordinal();
        int south = Direction.SOUTH.ordinal();
        int west = Direction.WEST.ordinal();
        int east = Direction.EAST.ordinal();
        CHelper.whatever(north, south, west, east, this.renderChunkNeighbours);
    }
    
}
