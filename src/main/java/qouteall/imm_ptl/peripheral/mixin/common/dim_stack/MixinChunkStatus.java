package qouteall.imm_ptl.peripheral.mixin.common.dim_stack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;

import java.util.List;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    @Inject(
        method = "method_17033", at = @At("HEAD"),
        remap = false
    )
    private static void redirectPopulateEntities(
        ChunkStatus var1, ServerLevel world, ChunkGenerator var3, List<ChunkAccess> var4, ChunkAccess chunk,
        CallbackInfo ci
    ) {
        DimStackManagement.replaceBedrock(world, chunk);
    }
}
