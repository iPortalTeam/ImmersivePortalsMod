package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;

import java.util.Queue;
import java.util.concurrent.Executor;

@Mixin(ChunkBuilder.class)
public class MixinChunkBuilder {
    @Shadow
    @Final
    @Mutable
    private Queue<BlockBufferBuilderStorage> threadBuffers;
    
    @Shadow
    private volatile int bufferCount;
    
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I",
            ordinal = 1
        )
    )
    private int redirectMax(int a, int b) {
        if (IPGlobal.enableSharedBlockMeshBuffers) {
            return 0;
        }
        return Math.max(a, b);
    }
    
    // inject on constructor seems to be not working normally
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/thread/TaskExecutor;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/thread/TaskExecutor;"
        )
    )
    private TaskExecutor redirectCreate(Executor executor, String name) {
        if (IPGlobal.enableSharedBlockMeshBuffers) {
            Validate.isTrue(threadBuffers.size() == 0);
            threadBuffers = SharedBlockMeshBuffers.getThreadBuffers();
            bufferCount = threadBuffers.size();
        }
        
        return TaskExecutor.create(executor, name);
    }

//    @Inject(
//        method = "<init>",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/util/thread/TaskExecutor;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/thread/TaskExecutor;"
//        )
//    )
//    private void onInit(
//        World world, WorldRenderer worldRenderer,
//        Executor executor, boolean is64Bits, BlockBufferBuilderStorage buffers,
//        CallbackInfo ci
//    ) {
//
//    }
    
    
}
