package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;

//it will always be the same size as the main frame buffer
public class SecondaryFrameBuffer {
    public GlFramebuffer fb;
    
    public void prepare() {
        GlFramebuffer mainFrameBuffer = MinecraftClient.getInstance().getFramebuffer();
        int width = mainFrameBuffer.viewWidth;
        int height = mainFrameBuffer.viewHeight;
        if (fb == null) {
            fb = new GlFramebuffer(
                width, height,
                true,//has depth attachment
                MinecraftClient.IS_SYSTEM_MAC
            );
        }
        if (width != fb.viewWidth ||
            height != fb.viewHeight
        ) {
            fb.resize(
                width, height, MinecraftClient.IS_SYSTEM_MAC
            );
            Helper.log("Deferred buffer resized");
        }
    }
}
