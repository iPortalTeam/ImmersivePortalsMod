package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.ShaderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.List;

@Deprecated
public class AlternateSkyRenderer {
    public static void renderAlternateSky(MatrixStack matrixStack, float f) {
        ClientWorld world = MinecraftClient.getInstance().world;
        
        if (shouldRenderBlackLid(world)) {
            renderBlackLid(matrixStack);
        }
    }
    
    private static boolean shouldRenderBlackLid(ClientWorld world) {
        List<GlobalTrackedPortal> globalPortals = ((IEClientWorld) world).getGlobalPortals();
        if (globalPortals == null) {
            return false;
        }
        return globalPortals.stream().anyMatch(portal ->
            portal.getY() > 128 && portal.dimensionTo == DimensionType.THE_END
        );
    }
    
    private static void renderBlackLid(MatrixStack matrixStack) {
        RenderSystem.disableTexture();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableFog();
        RenderSystem.shadeModel(7425);
        
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        
        bufferBuilder.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        float colorR = 18 / 255.0f;
        float colorG = 13 / 255.0f;
        float colorB = 26 / 255.0f;
        float colorA = 0;
        
        bufferBuilder.vertex(100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, -100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 0, 100).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(0, 0, 100).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, -100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(-100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(-100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, -100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 0, -100).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(0, 0, -100).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, -100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 0, 100).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(0, 0, 100).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(-100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(-100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 0, -100).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.vertex(0, 0, -100).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(0, 100, 0).color(colorR, colorG, colorB, colorA).next();
        bufferBuilder.vertex(100, 0, 0).color(colorR, colorG, colorB, colorA).next();
        
        bufferBuilder.end();
        
        McHelper.runWithTransformation(matrixStack, () -> {
            if (CGlobal.shaderManager == null) {
                CGlobal.shaderManager = new ShaderManager();
            }
            
            double y = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().y;
            
            float origin = y > 256 ? (float) ((256.0 - y) / 256.0) : 0;
            CGlobal.shaderManager.loadGradientSkyShader(origin);
            BufferRenderer.draw(bufferBuilder);
            CGlobal.shaderManager.unloadShader();
        });
        
        //reset gl states
        RenderLayer.getBlockLayers().get(0).startDrawing();
        RenderLayer.getBlockLayers().get(0).endDrawing();
        RenderSystem.depthMask(true);
    }
}
