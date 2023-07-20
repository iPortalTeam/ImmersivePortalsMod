package qouteall.imm_ptl.core.compat.iris_compatibility;

import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.ShadowRenderer;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.LevelRenderer;
import qouteall.q_misc_util.Helper;

import org.jetbrains.annotations.Nullable;
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
        
        public Object getPipeline(LevelRenderer worldRenderer) {
            return null;
        }
        
        public void setPipeline(LevelRenderer worldRenderer, Object pipeline) {
        
        }
        
        public void reloadPipelines() {}
    
        @Nullable
        public String getShaderpackName() {
            return null;
        }
    }
    
    public static class OnIrisPresent extends Invoker {
        
        private Field worldRendererPipelineField = Helper.noError(() -> {
            Field field = LevelRenderer.class.getDeclaredField("pipeline");
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
        public Object getPipeline(LevelRenderer worldRenderer) {
            return Helper.noError(() ->
                ((WorldRenderingPipeline) worldRendererPipelineField.get(worldRenderer))
            );
        }
        
        // the pipeline switching is unnecessary when using shaders
        // but still necessary with shaders disabled
        @Override
        public void setPipeline(LevelRenderer worldRenderer, Object pipeline) {
            Helper.noError(() -> {
                worldRendererPipelineField.set(worldRenderer, pipeline);
                return null;
            });
        }
        
        @Override
        public void reloadPipelines() {
            Iris.getPipelineManager().destroyPipeline();
        }
    
        @Nullable
        @Override
        public String getShaderpackName() {
            return Iris.getCurrentPackName();
        }
    }
    
    public static Invoker invoker = new Invoker();
}
