package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

import javax.annotation.Nullable;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class GuiPortalRendering {
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    @Nullable
    private static RenderTarget renderingFrameBuffer = null;
    
    @Nullable
    public static RenderTarget getRenderingFrameBuffer() {
        return renderingFrameBuffer;
    }
    
    public static boolean isRendering() {
        return getRenderingFrameBuffer() != null;
    }
    
    private static void renderWorldIntoFrameBuffer(
        WorldRenderInfo worldRenderInfo,
        RenderTarget framebuffer
    ) {
        RenderStates.basicProjectionMatrix = null;
        
        CHelper.checkGlError();
        
        ((IECamera) RenderStates.originalCamera).resetState(
            worldRenderInfo.cameraPos, worldRenderInfo.world
        );
        
        Validate.isTrue(renderingFrameBuffer == null);
        renderingFrameBuffer = framebuffer;
        
        MyRenderHelper.restoreViewPort();
        
        RenderTarget mcFb = MyGameRenderer.client.getMainRenderTarget();
        
        Validate.isTrue(mcFb != framebuffer);
        
        ((IEMinecraftClient) MyGameRenderer.client).setFrameBuffer(framebuffer);
        
        framebuffer.bindWrite(true);
        
        IPCGlobal.renderer.prepareRendering();
        
        IPCGlobal.renderer.invokeWorldRendering(worldRenderInfo);
        
        IPCGlobal.renderer.finishRendering();
        
        ((IEMinecraftClient) MyGameRenderer.client).setFrameBuffer(mcFb);
        
        mcFb.bindWrite(true);
        
        renderingFrameBuffer = null;
        
        MyRenderHelper.restoreViewPort();
        
        CHelper.checkGlError();
        
        RenderStates.basicProjectionMatrix = null;
    }
    
    private static final HashMap<RenderTarget, WorldRenderInfo> renderingTasks = new HashMap<>();
    
    public static void submitNextFrameRendering(
        WorldRenderInfo worldRenderInfo,
        RenderTarget renderTarget
    ) {
        Validate.isTrue(!renderingTasks.containsKey(renderTarget));
        
        RenderTarget mcFB = Minecraft.getInstance().getMainRenderTarget();
        if (renderTarget.width != mcFB.width || renderTarget.height != mcFB.height) {
            renderTarget.resize(mcFB.width, mcFB.height, true);
            Helper.log("Resized Framebuffer for GUI Portal Rendering");
        }
        
        renderingTasks.put(renderTarget, worldRenderInfo);
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
