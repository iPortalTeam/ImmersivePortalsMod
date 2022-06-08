package qouteall.imm_ptl.core.render.optimization;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.function.Consumer;

public class GLResourceCache {
    public static Minecraft client = Minecraft.getInstance();
    
    private final Consumer<int[]> generator;
    private final IntList bufferIds = new IntArrayList();
    
    public static GLResourceCache bufferCache = new GLResourceCache(GL15::glGenBuffers);
    public static GLResourceCache vertexArrayCache = new GLResourceCache(GL30::glGenVertexArrays);
    
    public GLResourceCache(Consumer<int[]> generator) {
        this.generator = generator;
    }
    
    public int getNewResourceId() {
        if (bufferIds.isEmpty()) {
            reserve(1000);
        }
        
        int taken = bufferIds.removeInt(bufferIds.size() - 1);
        return taken;
    }
    
    public static void init() {
    
    }
    
    private void reserve(int num) {
        int[] buf = new int[num];
        generator.accept(buf);
        bufferIds.addElements(bufferIds.size(), buf);
    }
    
    private int getExpectedBufferSize() {
        int viewDistance = client.options.getEffectiveRenderDistance();
        int diameter = viewDistance * 2 + 1;
        
        //every column has 16 sections, every section has 5 layers
        return diameter * diameter * 16 * 5 * 4;
    }
}
