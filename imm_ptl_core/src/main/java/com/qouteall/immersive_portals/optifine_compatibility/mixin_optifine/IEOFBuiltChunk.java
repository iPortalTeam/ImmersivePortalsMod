package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk")
public interface IEOFBuiltChunk {
    @Invoker("setRenderChunkNeighbour")
    void ip_setRenderChunkNeighbour(Direction facing, ChunkBuilder.BuiltChunk neighbour);
    
}
