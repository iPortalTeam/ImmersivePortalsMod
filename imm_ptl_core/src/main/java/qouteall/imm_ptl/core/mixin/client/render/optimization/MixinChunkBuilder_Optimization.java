package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.OFInterface;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.q_misc_util.Helper;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

@Mixin(ChunkBuilder.class)
public abstract class MixinChunkBuilder_Optimization {
    // TODO recover
    
//    @Shadow
//    @Final
//    @Mutable
//    private Queue<BlockBufferBuilderStorage> threadBuffers;
//
//    @Shadow
//    private volatile int bufferCount;
//
//    @Shadow
//    private volatile int queuedTaskCount;
//
//    @Shadow
//    @Final
//    private Executor executor;
//
//    @Shadow
//    @Final
//    private TaskExecutor<Runnable> mailbox;
//
//    @Redirect(
//        method = "<init>",
//        at = @At(
//            value = "INVOKE",
//            target = "Ljava/lang/Math;max(II)I",
//            ordinal = 1
//        ),
//        require = 0
//    )
//    private int redirectMax(int a, int b) {
//        Validate.isTrue(!OFInterface.isOptifinePresent);
//        if (IPGlobal.enableSharedBlockMeshBuffers) {
//            return 0;
//        }
//        return Math.max(a, b);
//    }
//
//    // inject on constructor seems to be not working normally
//    @Redirect(
//        method = "<init>",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/util/thread/TaskExecutor;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/thread/TaskExecutor;"
//        ),
//        require = 0
//    )
//    private TaskExecutor redirectCreate(Executor executor, String name) {
//        if (IPGlobal.enableSharedBlockMeshBuffers && !OFInterface.isOptifinePresent) {
//            Validate.isTrue(threadBuffers.size() == 0);
//            threadBuffers = SharedBlockMeshBuffers.getThreadBuffers();
//            bufferCount = threadBuffers.size();
//        }
//
//        return TaskExecutor.create(executor, name);
//    }
//
//
//    /**
//     * @author qouteall
//     * @reason hard to do with injects and redirects
//     * VanillaCopy
//     */
//    @Overwrite
//    private void scheduleRunTasks() {
//        if (this.threadBuffers.isEmpty()) {
//            IPGlobal.clientTaskList.addTask(() -> {
//                mailbox.send(this::scheduleRunTasks);
//                return true;
//            });
//            return;
//        }
//
//        ChunkBuilder.BuiltChunk.Task task = (ChunkBuilder.BuiltChunk.Task) this.rebuildQueue.poll();
//        if (task != null) {
//            // threadBuffers is being accessed concurrently so pooling may fail
//            BlockBufferBuilderStorage blockBufferBuilderStorage = (BlockBufferBuilderStorage) this.threadBuffers.poll();
//            if (blockBufferBuilderStorage == null) {
//                rebuildQueue.add(task);
//                IPGlobal.clientTaskList.addTask(() -> {
//                    mailbox.send(this::scheduleRunTasks);
//                    return true;
//                });
//                return;
//            }
//            this.queuedTaskCount = this.rebuildQueue.size();
//            this.bufferCount = this.threadBuffers.size();
//            CompletableFuture.runAsync(() -> {
//            }, this.executor).thenCompose((void_) -> {
//                return task.run(blockBufferBuilderStorage);
//            }).whenComplete((result, throwable) -> {
//                if (throwable != null) {
//                    CrashReport crashReport = CrashReport.create(throwable, "Batching chunks");
//                    MinecraftClient.getInstance().setCrashReport(MinecraftClient.getInstance().addDetailsToCrashReport(crashReport));
//                }
//                else {
//                    this.mailbox.send(() -> {
//                        if (result == ChunkBuilder.Result.SUCCESSFUL) {
//                            blockBufferBuilderStorage.clear();
//                        }
//                        else {
//                            blockBufferBuilderStorage.reset();
//                        }
//
//                        this.threadBuffers.add(blockBufferBuilderStorage);
//                        this.bufferCount = this.threadBuffers.size();
//                        this.scheduleRunTasks();
//                    });
//                }
//            });
//        }
//    }
}
