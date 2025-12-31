package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import qouteall.q_misc_util.Helper;

//it will always be the same size as the main frame buffer
public class SecondaryFrameBuffer {
    public TextureTarget fb;
    
    public void prepare() {
        RenderTarget mainFrameBuffer = Minecraft.getInstance().getMainRenderTarget();
        int width = mainFrameBuffer.width;
        int height = mainFrameBuffer.height;
        prepare(width, height);
    }
    
    public void prepare(int width, int height) {
        if (fb == null) {
            fb = new TextureTarget(
                "imm_ptl_secondary",
                width, height,
                true//has depth attachment
            );
            Helper.log("Secondary Framebuffer init");
        }
        if (width != fb.width ||
            height != fb.height
        ) {
            fb.resize(width, height);
            Helper.log("Secondary Framebuffer resized");
        }
    }
    
    
}
