package com.qouteall.imm_ptl_peripheral.test;

import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.render.GuiPortalRendering;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.context_management.WorldRendering;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class GuiPortalTest {
    private static Framebuffer frameBuffer;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void open() {
        if (frameBuffer == null) {
            frameBuffer = new Framebuffer(500, 500, true, true);
        }
        
        client.openScreen(new TestScreen());
    }
    
    public static class TestScreen extends Screen {
        
        public TestScreen() {
            super(new LiteralText("GUI Portal Test"));
        }
        
        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.render(matrices, mouseX, mouseY, delta);
            
            Matrix4f cameraTransformation = new Matrix4f();
            cameraTransformation.loadIdentity();
            
            WorldRendering worldRendering = new WorldRendering(
                ClientWorldLoader.getWorld(World.OVERWORLD),
                client.player.getPos().add(0, 5, 0),
                cameraTransformation, null,
                client.options.viewDistance, true
            );
            GuiPortalRendering.submitNextFrameRendering(worldRendering, frameBuffer);
            
            MyRenderHelper.drawFramebuffer(
                frameBuffer,
                false, false,
                0, 500, 0, 500
            );
            
            drawCenteredText(
                matrices, this.textRenderer, this.title, this.width / 2, 70, 16777215
            );
        }
        
        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
