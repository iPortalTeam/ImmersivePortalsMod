package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.McHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class AlternateSky {
    public static void renderAlternateSky(MatrixStack matrixStack, float f) {
        ClientWorld world = MinecraftClient.getInstance().world;

//        VertexBuffer.unbind();
        
        RenderSystem.disableTexture();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableFog();
        RenderSystem.shadeModel(7425);
        
        Vec3d skyColor = world.method_23777(
            MinecraftClient.getInstance().gameRenderer.getCamera().getBlockPos(),
            f
        );
        float[] skyColor1 = world.dimension.getBackgroundColor(world.getSkyAngle(f), f);
        
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        
        bufferBuilder.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        float topR = (float) skyColor.x;
        float topG = (float) skyColor.y;
        float topB = (float) skyColor.z;
        float topA = 0;
        float bottomR = 0;
        float bottomG = 0;
        float bottomB = 0;
        float bottomA = 1f;
        
        bufferBuilder.vertex(1, 0, 0).color(bottomR, bottomG, bottomB, bottomA).next();
        bufferBuilder.vertex(0, -1, 0).color(topR, topG, topB, topA).next();
        bufferBuilder.vertex(0, 0, 1).color(bottomR, bottomG, bottomB, bottomA).next();
        
        bufferBuilder.vertex(0, 0, 1).color(bottomR, bottomG, bottomB, bottomA).next();
        bufferBuilder.vertex(0, -1, 0).color(topR, topG, topB, topA).next();
        bufferBuilder.vertex(-1, 0, 0).color(bottomR, bottomG, bottomB, bottomA).next();
        
        bufferBuilder.vertex(-1, 0, 0).color(bottomR, bottomG, bottomB, bottomA).next();
        bufferBuilder.vertex(0, -1, 0).color(topR, topG, topB, topA).next();
        bufferBuilder.vertex(0, 0, -1).color(bottomR, bottomG, bottomB, bottomA).next();
        
        bufferBuilder.vertex(0, 0, -1).color(bottomR, bottomG, bottomB, bottomA).next();
        bufferBuilder.vertex(0, -1, 0).color(topR, topG, topB, topA).next();
        bufferBuilder.vertex(1, 0, 0).color(bottomR, bottomG, bottomB, bottomA).next();
        
        bufferBuilder.end();
        
        matrixStack.push();
        matrixStack.translate(0, 0.3, 0);
        McHelper.runWithTransformation(matrixStack, () -> {
            BufferRenderer.draw(bufferBuilder);
        });
        matrixStack.pop();
        
        //reset gl states
        RenderLayer.getBlockLayers().get(0).startDrawing();
        RenderLayer.getBlockLayers().get(0).endDrawing();
        RenderSystem.depthMask(true);
    }
}
