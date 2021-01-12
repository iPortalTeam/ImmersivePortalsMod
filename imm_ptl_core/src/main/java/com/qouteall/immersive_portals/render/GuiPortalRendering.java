package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.WorldRendering;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class GuiPortalRendering {
    @Nullable
    private static Framebuffer renderingFrameBuffer = null;
    
    @Nullable
    public static Framebuffer getRenderingFrameBuffer() {
        return renderingFrameBuffer;
    }
    
    public static boolean isRendering(){
        return getRenderingFrameBuffer() != null;
    }
    
    public static void renderWorldIntoFrameBuffer(
        WorldRendering worldRendering,
        Framebuffer framebuffer
    ) {
        RenderStates.projectionMatrix = null;
        
        CHelper.checkGlError();
        
        Validate.isTrue(renderingFrameBuffer == null);
        renderingFrameBuffer = framebuffer;
        
        MyRenderHelper.restoreViewPort();
        
        Framebuffer mcFb = MyGameRenderer.client.getFramebuffer();
        
        Validate.isTrue(mcFb != framebuffer);
        
        ((IEMinecraftClient) MyGameRenderer.client).setFrameBuffer(framebuffer);
        
        framebuffer.beginWrite(true);
        
        CGlobal.renderer.prepareRendering();
        
        CGlobal.renderer.invokeWorldRendering(worldRendering);
        
        CGlobal.renderer.finishRendering();
        
        ((IEMinecraftClient) MyGameRenderer.client).setFrameBuffer(mcFb);
        
        mcFb.beginWrite(true);
    
        renderingFrameBuffer = null;
        
        MyRenderHelper.restoreViewPort();
        
        CHelper.checkGlError();
    
        RenderStates.projectionMatrix = null;
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
            renderWorldIntoFrameBuffer(
                worldRendering, frameBuffer
            );
        });
        renderingTasks.clear();
    }
}
