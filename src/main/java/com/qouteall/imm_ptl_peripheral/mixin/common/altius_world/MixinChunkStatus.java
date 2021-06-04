package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    // TODO recover bedrock replacing
    
//    @Inject(
//        method = "*",
//        at = @At(
//            value = "INVOKE",
//            shift = At.Shift.AFTER,
//            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;buildSurface(Lnet/minecraft/world/ChunkRegion;Lnet/minecraft/world/chunk/Chunk;)V"
//        )
//    )
//    private static void redirectBuildSurface(
//        ChunkGenerator generator, ChunkRegion region, Chunk chunk,
//        CallbackInfo ci
//    ) {
//        AltiusInfo.replaceBedrock(region, chunk);
//    }
    
}
