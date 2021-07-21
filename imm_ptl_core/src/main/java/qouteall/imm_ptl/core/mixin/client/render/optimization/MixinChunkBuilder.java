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
import qouteall.imm_ptl.core.OFInterface;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.q_misc_util.Helper;

import java.util.Queue;
import java.util.concurrent.Executor;

@Mixin(ChunkBuilder.class)
public abstract class MixinChunkBuilder {
    @Shadow
    @Final
    @Mutable
    private Queue<BlockBufferBuilderStorage> threadBuffers;
    
    @Shadow
    private volatile int bufferCount;
    
    @Shadow
    protected abstract void scheduleRunTasks();
    
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I",
            ordinal = 1
        ),
        require = 0
    )
    private int redirectMax(int a, int b) {
        if (IPGlobal.enableSharedBlockMeshBuffers && !OFInterface.isOptifinePresent) {
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
        ),
        require = 0
    )
    private TaskExecutor redirectCreate(Executor executor, String name) {
        if (IPGlobal.enableSharedBlockMeshBuffers && !OFInterface.isOptifinePresent) {
            Validate.isTrue(threadBuffers.size() == 0);
            threadBuffers = SharedBlockMeshBuffers.getThreadBuffers();
            bufferCount = threadBuffers.size();
        }
        
        return TaskExecutor.create(executor, name);
    }
    
    private boolean portal_isInRaceCondition = false;
    
    // it firstly check empty and then pool an element
    // because this queue may be manipulated concurrently, it may pool a null
    // if so, retry next tick
    @Redirect(
        method = "scheduleRunTasks",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Queue;poll()Ljava/lang/Object;"
        )
    )
    private Object redirectPool(Queue queue) {
        Object result = queue.poll();
        portal_isInRaceCondition = result == null;
        return result;
    }
    
    @Inject(
        method = "scheduleRunTasks",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
        ),
        cancellable = true
    )
    private void onScheduleTasks(CallbackInfo ci) {
        if (portal_isInRaceCondition) {
            portal_isInRaceCondition = false;
            Helper.log("ChunkBuilder race condition triggered");
            IPGlobal.clientTaskList.addTask(() -> {
                scheduleRunTasks();
                return true;
            });
            ci.cancel();
        }
    }
    
}
