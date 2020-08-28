package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

//it will always be the same size as the main frame buffer
public class SecondaryFrameBuffer {
    public Framebuffer fb;
    
    public void prepare() {
        Framebuffer mainFrameBuffer = MinecraftClient.getInstance().getFramebuffer();
        int width = mainFrameBuffer.viewportWidth;
        int height = mainFrameBuffer.viewportHeight;
        prepare(width, height);
    }
    
    public void prepare(int width, int height) {
        if (fb == null) {
            fb = new Framebuffer(
                width, height,
                true,//has depth attachment
                MinecraftClient.IS_SYSTEM_MAC
            );
        }
        if (width != fb.viewportWidth ||
            height != fb.viewportHeight
        ) {
            fb.resize(
                width, height, MinecraftClient.IS_SYSTEM_MAC
            );
            Helper.log("Deferred buffer resized");
        }
    }
    
    
}
