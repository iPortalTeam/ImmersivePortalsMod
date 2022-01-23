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
        int width = mainFrameBuffer.viewWidth;
        int height = mainFrameBuffer.viewHeight;
        prepare(width, height);
    }
    
    public void prepare(int width, int height) {
        if (fb == null) {
            fb = new TextureTarget(
                width, height,
                true,//has depth attachment
                Minecraft.ON_OSX
            );
            fb.checkStatus();
            Helper.log("Deferred buffer init");
        }
        if (width != fb.viewWidth ||
            height != fb.viewHeight
        ) {
            fb.resize(
                width, height, Minecraft.ON_OSX
            );
            fb.checkStatus();
            Helper.log("Deferred buffer resized");
        }
    }
    
    
}
