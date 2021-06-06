package qouteall.imm_ptl.peripheral.mixin.common.altius_world;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.peripheral.altius_world.AltiusInfo;

import java.util.List;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    @Inject(
        method = "method_16566", at = @At("HEAD")
    )
    private static void redirectPopulateEntities(
        ChunkStatus targetStatus, ServerWorld world,
        ChunkGenerator chunkGenerator, List<Chunk> list, Chunk chunk,
        CallbackInfo ci
    ) {
        AltiusInfo.replaceBedrock(world, chunk);
    }
    
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
