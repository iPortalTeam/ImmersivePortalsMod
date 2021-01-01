package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(BuiltChunkStorage.class)
public interface IEOFBuiltChunkStorage {
    @Accessor("mapVboRegions")
    Map<ChunkPos,Object> ip_getMapVboRegions();
    
    
}
