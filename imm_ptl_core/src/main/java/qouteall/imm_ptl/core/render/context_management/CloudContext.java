package qouteall.imm_ptl.core.render.context_management;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;

/**
 * {@link net.minecraft.client.render.WorldRenderer#renderClouds(MatrixStack, float, double, double, double)}
 */
public class CloudContext {
    
    //keys
    public int lastCloudsBlockX = 0;
    public int lastCloudsBlockY = 0;
    public int lastCloudsBlockZ = 0;
    public ResourceKey<Level> dimension = null;
    public Vec3 cloudColor;
    
    public VertexBuffer cloudsBuffer = null;
    
    public static final ArrayList<CloudContext> contexts = new ArrayList<>();
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(CloudContext::cleanup);
        ClientWorldLoader.clientDimensionDynamicRemoveSignal.connect(dim -> cleanup());
    }
    
    public CloudContext() {
    
    }
    
    private static void cleanup() {
        for (CloudContext context : contexts) {
            context.dispose();
        }
        contexts.clear();
    }
    
    public void dispose() {
        if (cloudsBuffer != null) {
            cloudsBuffer.close();
            cloudsBuffer = null;
        }
    }
    
    @Nullable
    public static CloudContext findAndTakeContext(
        int lastCloudsBlockX, int lastCloudsBlockY, int lastCloudsBlockZ,
        ResourceKey<Level> dimension, Vec3 cloudColor
    ) {
        int i = Helper.indexOf(contexts, c ->
            c.lastCloudsBlockX == lastCloudsBlockX &&
                c.lastCloudsBlockY == lastCloudsBlockY &&
                c.lastCloudsBlockZ == lastCloudsBlockZ &&
                c.dimension == dimension &&
                c.cloudColor.distanceToSqr(cloudColor) < 2.0E-4D
        );
        
        if (i == -1) {
            return null;
        }
        
        CloudContext result = contexts.get(i);
        contexts.remove(i);
        
        return result;
    }
    
    public static void appendContext(CloudContext context) {
        contexts.add(context);
        
        if (contexts.size() > 15) {
            contexts.remove(0).dispose();
        }
    }
}
