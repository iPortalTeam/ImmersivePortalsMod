package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.thread.ProcessorMailbox;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.q_misc_util.Helper;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher_Optimization {
    @Shadow
    private volatile int freeBufferCount;
    
    @Mutable
    @Shadow
    @Final
    private Queue<ChunkBufferBuilderPack> freeBuffers;
    
    @Shadow
    @Final
    private ProcessorMailbox<Runnable> mailbox;
    
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
        if (IPGlobal.enableSharedBlockMeshBuffers) {
            return 0;
        }
        return Math.max(a, b);
    }
    
    // inject on constructor seems to be not working normally, so use redirect
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/thread/ProcessorMailbox;create(Ljava/util/concurrent/Executor;Ljava/lang/String;)Lnet/minecraft/util/thread/ProcessorMailbox;"
        ),
        require = 0
    )
    private ProcessorMailbox<Runnable> redirectCreate(Executor dispatcher, String name) {
        if (IPGlobal.enableSharedBlockMeshBuffers) {
            Validate.isTrue(freeBufferCount == 0);
            freeBuffers = SharedBlockMeshBuffers.getThreadBuffers();
            freeBufferCount = freeBuffers.size();
        }
        
        return ProcessorMailbox.create(dispatcher, name);
    }
    
    @Redirect(
        method = "runTask",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Queue;poll()Ljava/lang/Object;"
        )
    )
    private <E> E redirectPoll(Queue<E> queue) {
        if (!IPGlobal.enableSharedBlockMeshBuffers) {
            return queue.poll();
        }
        
        E element = queue.poll();
        
        if (element != null) {
            return element;
        }
        
        // there is a tiny time gap between reading the size and pooling the element
        // another thread may poll an element during this time
        // so it may fail to poll
        // temporarily create a new pack and delete one pack from the queue later
        
        ChunkBufferBuilderPack newPack = new ChunkBufferBuilderPack();
        
        // remove the new buffer pack
        mailbox.tell(this::ip_disposeOneBufferPack);
        
        Helper.log("The chunk render dispatcher buffer packs are drained. Temporarily create a new buffer pack");
        
        return (E) newPack;
    }
    
    private void ip_disposeOneBufferPack() {
        ChunkBufferBuilderPack pack = freeBuffers.poll();
        if (pack == null) {
            mailbox.tell(this::ip_disposeOneBufferPack);
            Helper.log("Tries to dispose the buffer pack again");
        }
        else {
            freeBufferCount = freeBuffers.size();
            Helper.log("Disposed the temporary buffer pack. " + freeBufferCount);
        }
    }
}
