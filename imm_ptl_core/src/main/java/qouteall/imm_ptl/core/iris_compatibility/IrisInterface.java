package qouteall.imm_ptl.core.iris_compatibility;

import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.render.WorldRenderer;
import qouteall.q_misc_util.Helper;

import java.lang.reflect.Field;

public class IrisInterface {
    
    public static class Invoker {
        public boolean isIrisPresent() {
            return false;
        }
        
        public boolean isShaders() {
            return false;
        }
        
        public WorldRenderingPipeline getPipeline(WorldRenderer worldRenderer) {
            return null;
        }
        
        public void setPipeline(WorldRenderer worldRenderer, WorldRenderingPipeline pipeline) {
        
        }
    }
    
    public static class OnIrisPresent extends Invoker {
        
        private Field worldRendererPipelineField = Helper.noError(() -> {
            Field field = WorldRenderer.class.getDeclaredField("pipeline");
            field.setAccessible(true);
            return field;
        });
        
        @Override
        public boolean isIrisPresent() {
            return true;
        }
        
        @Override
        public boolean isShaders() {
            return Iris.getCurrentPack().isPresent();
        }
        
        @Override
        public WorldRenderingPipeline getPipeline(WorldRenderer worldRenderer) {
            return Helper.noError(() ->
                ((WorldRenderingPipeline) worldRendererPipelineField.get(worldRenderer))
            );
        }
        
        @Override
        public void setPipeline(WorldRenderer worldRenderer, WorldRenderingPipeline pipeline) {
            Helper.noError(() -> {
                worldRendererPipelineField.set(worldRenderer, pipeline);
                return null;
            });
        }
    }
    
    public static Invoker invoker = new Invoker();
}
