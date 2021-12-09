package qouteall.imm_ptl.core.render.optimization;

import com.google.common.collect.Queues;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.Queue;

/**
 * This optimization makes that different dimensions of ChunkBuilders
 * use the same BlockBufferBuilderStorage
 * If enabled, it can avoid OutOfMemory issues
 * However in normal cases it increase memory usage by 10 MB, for unknown reasons
 */
@Deprecated
public class SharedBlockMeshBuffers {
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(SharedBlockMeshBuffers::cleanup);
    }
    
    /**
     * {@link net.minecraft.client.render.chunk.ChunkBuilder}
     */
    private static Queue<BlockBufferBuilderStorage> threadBuffers;
    
    public static Queue<BlockBufferBuilderStorage> getThreadBuffers() {
        if (threadBuffers == null) {
            createThreadBuffers();
        }
        return threadBuffers;
    }
    
    private static void createThreadBuffers() {
        Validate.isTrue(IPGlobal.enableSharedBlockMeshBuffers);
        
        int totalExpectedBufferSize =
            RenderLayer.getBlockLayers().stream().mapToInt(RenderLayer::getExpectedBufferSize).sum();
        int bufferCountEstimation = Math.max(
            1,
            (int) ((double) Runtime.getRuntime().maxMemory() * 0.3D) / (totalExpectedBufferSize * 4) - 1
        );
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        boolean is64Bits = MinecraftClient.getInstance().is64Bit();
        int effectiveProcessors = is64Bits ? availableProcessors : Math.min(availableProcessors, 4);
        int bufferCount = Math.max(1, Math.min(effectiveProcessors, bufferCountEstimation));
        
        ArrayList<BlockBufferBuilderStorage> list = new ArrayList<>();
        
        try {
            for (int m = 0; m < bufferCount; ++m) {
                list.add(new BlockBufferBuilderStorage());
            }
        }
        catch (OutOfMemoryError error) {
            error.printStackTrace();
            
            String errorMessage = String.format(
                "[Immersive Portals] Allocated only %s/%s block mesh buffers. Memory seems not enough.",
                list.size(), bufferCount
            );
            
            Helper.err(errorMessage);
            
            CHelper.printChat(errorMessage);
            
            int n = Math.min(list.size() * 2 / 3, list.size() - 1);
            
            for (int i = 0; i < n; ++i) {
                list.remove(list.size() - 1);
            }
            
            System.gc();
        }
        
        threadBuffers = Queues.newConcurrentLinkedQueue(list);
    }
    
    private static void cleanup() {
        // the buffers are not GL resources and can be collected by GC
        if (threadBuffers != null) {
            threadBuffers = null;
        }
    }
}
