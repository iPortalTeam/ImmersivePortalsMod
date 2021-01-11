package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.render.context_management.WorldRendering;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class GuiPortalRendering {
    
    public static class RenderingTask {
        public final WorldRendering worldRendering;
        public final Framebuffer renderTarget;
        
        public RenderingTask(WorldRendering worldRendering, Framebuffer renderTarget) {
            this.worldRendering = worldRendering;
            this.renderTarget = renderTarget;
        }
    }
    
    private static final HashMap<Framebuffer, WorldRendering> renderingTasks = new HashMap<>();
    
    public static void submitNextFrameRendering(
        WorldRendering worldRendering,
        Framebuffer renderTarget
    ) {
        Validate.isTrue(!renderingTasks.containsKey(renderTarget));
        
        renderingTasks.put(renderTarget, worldRendering);
    }
    
    // Not API
    public static void onGameRenderEnd() {
        renderingTasks.forEach((frameBuffer, worldRendering) -> {
            MyGameRenderer.renderWorldIntoFrameBuffer(
                worldRendering, frameBuffer
            );
        });
        renderingTasks.clear();
    }
}
