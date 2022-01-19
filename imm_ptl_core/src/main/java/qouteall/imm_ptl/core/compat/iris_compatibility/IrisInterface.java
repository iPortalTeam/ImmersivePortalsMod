package qouteall.imm_ptl.core.compat.iris_compatibility;

import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.ShadowRenderer;
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
        
        public boolean isRenderingShadowMap() {
            return false;
        }
        
        public Object getPipeline(WorldRenderer worldRenderer) {
            return null;
        }
        
        // TODO check whether it's necessary
        public void setPipeline(WorldRenderer worldRenderer, Object pipeline) {
        
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
        public boolean isRenderingShadowMap() {
            return ShadowRenderer.ACTIVE;
        }
        
        @Override
        public Object getPipeline(WorldRenderer worldRenderer) {
            return Helper.noError(() ->
                ((WorldRenderingPipeline) worldRendererPipelineField.get(worldRenderer))
            );
        }
        
        @Override
        public void setPipeline(WorldRenderer worldRenderer, Object pipeline) {
            Helper.noError(() -> {
                worldRendererPipelineField.set(worldRenderer, pipeline);
                return null;
            });
        }
    
    }
    
    public static Invoker invoker = new Invoker();
}
