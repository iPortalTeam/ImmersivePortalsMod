package com.qouteall.imm_ptl_peripheral.test;

import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.render.GuiPortalRendering;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class ExampleGuiPortalRendering {
    private static Framebuffer frameBuffer;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void open() {
        if (frameBuffer == null) {
            frameBuffer = new Framebuffer(1000, 1000, true, true);
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
            cameraTransformation.multiply(
                DQuaternion.rotationByDegrees(
                    new Vec3d(1, 1, 0).normalize(),
                    ((double) ((client.world.getTime() % 500) / 500.0) + RenderStates.tickDelta / 500.0) * 360
                ).toMcQuaternion()
            );
            
            WorldRenderInfo worldRenderInfo = new WorldRenderInfo(
                ClientWorldLoader.getWorld(World.OVERWORLD),
                client.player.getPos().add(0, 3, 0),
                cameraTransformation, null,
                client.options.viewDistance, true
            );
            GuiPortalRendering.submitNextFrameRendering(worldRenderInfo, frameBuffer);
            
            int h = client.getWindow().getFramebufferHeight();
            int w = client.getWindow().getFramebufferWidth();
            float sideLen = h * 0.6f;
            
            MyRenderHelper.drawFramebuffer(
                frameBuffer,
                false, false,
                w * 0.2f,
                w * 0.8f,
                h * 0.2f,
                h * 0.8f
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
