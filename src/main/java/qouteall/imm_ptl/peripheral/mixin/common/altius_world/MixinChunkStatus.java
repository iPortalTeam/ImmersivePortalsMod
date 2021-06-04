package qouteall.imm_ptl.peripheral.mixin.common.altius_world;

import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;

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
