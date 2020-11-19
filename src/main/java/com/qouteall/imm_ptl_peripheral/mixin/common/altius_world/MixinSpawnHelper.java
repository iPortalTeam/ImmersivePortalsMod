package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnHelper.class)
public class MixinSpawnHelper {
    
    //avoid spawning on top of nether in altius world
    //normally mob cannot spawn on bedrock but altius replaces it with obsidian
    @Redirect(
        method = "getSpawnPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/WorldChunk;sampleHeightmap(Lnet/minecraft/world/Heightmap$Type;II)I"
        )
    )
    private static int redirectGetTopY(
        WorldChunk chunk,
        Heightmap.Type type,
        int x,
        int z
    ) {
        int height = chunk.sampleHeightmap(type, x, z);
        int dimHeight = chunk.getWorld().getDimensionHeight();
        if (AltiusGameRule.getIsDimensionStack()) {
            if (chunk.getWorld().getRegistryKey() == World.NETHER) {
                return Math.min(height, dimHeight - 3);
            }
        }
        return height;
    }
}
