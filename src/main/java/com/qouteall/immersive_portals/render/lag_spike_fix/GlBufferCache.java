package com.qouteall.immersive_portals.render.lag_spike_fix;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.ObjectBuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL15;

public class GlBufferCache {
    public static MinecraftClient client = MinecraftClient.getInstance();
    
    private static ObjectBuffer<Integer> bufferIdBuffer = new ObjectBuffer<>(
        20 * 20 * 4,
        GL15::glGenBuffers,
        GL15::glDeleteBuffers
    );
    
    public static int getNewBufferId() {
        return bufferIdBuffer.takeObject();
    }
    
    public static void init() {
        ModMain.postClientTickSignal.connect(GlBufferCache::tick);
    }
    
    private static void tick() {
        if (!Global.cacheGlBuffer) {
            return;
        }
        
        if (client.world == null) {
            return;
        }
        
        if (client.player == null) {
            return;
        }
        
        client.getProfiler().push("gl_buffer_cache");
        
        int viewDistance = client.options.viewDistance;
        int diameter = viewDistance * 2 + 1;
        bufferIdBuffer.setCacheSize(
            diameter * diameter * 16 * 4 * 2
            //every column has 16 sections, every section has 4 layers
        );
        bufferIdBuffer.reserveObjectsByRatio(1.0 / 500);
        
        client.getProfiler().pop();
    }
}
