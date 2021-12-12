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
        method = "method_17033", at = @At("HEAD"),
        remap = false
    )
    private static void redirectPopulateEntities(
        ChunkStatus var1, ServerWorld world, ChunkGenerator var3, List<Chunk> var4, Chunk chunk,
        CallbackInfo ci
    ) {
        AltiusInfo.replaceBedrock(world, chunk);
    }
}
