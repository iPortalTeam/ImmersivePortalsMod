package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.ShaderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL20;

public class RendererCompatibleWithShaders extends PortalRenderer {
    private ShaderManager shaderManager;
    private boolean isEnabled = true;
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    protected void prepareStates() {
        if (shaderManager == null) {
            shaderManager = new ShaderManager();
        }
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        if (isRendering()) {
            Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
            if (cameraEntity != null) {
                Vec3d cameraPos = cameraEntity.getPos();
                if (portal.isInFrontOfPortal(cameraPos)) {
                    drawPortalViewTriangle(portal);
                }
            }
        }
    }
    
    @Override
    protected void doRenderPortal(Portal portal) {
        if (!isEnabled) {
            return;
        }
        
        if (isRendering()) {
            //currently only support one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal)) {
            return;
        }
        
        portalLayers.push(portal);
        
        OFGlobal.shaderContextManager.switchContextAndRun(
            portal.dimensionTo,
            () -> {
                OFGlobal.shaderContextManager.startupIfNecessary(portal.dimensionTo);
                manageCameraAndRenderPortalContent(portal);
                PerDimensionContext.bindGbuffersTextures.run();
            }
        );
        
        portalLayers.pop();
        
        //render the texture back to original frame buffer
        
        setupCameraTransformation();
        
        //switch back frame buffer
        EXTFramebufferObject.glBindFramebufferEXT(36160, PerDimensionContext.getDfb.get());
        
        shaderManager.loadContentShaderAndShaderVars();
        
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        
        drawPortalViewTriangle(portal);
        
        GlStateManager.enableBlend();
        
        shaderManager.unloadShader();
        
        PerDimensionContext.bindGbuffersTextures.run();
    }
    
    private boolean testShouldRenderPortal(Portal portal) {
        return renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture();
            setupCameraTransformation();
            GL20.glUseProgram(0);
            
            drawPortalViewTriangle(portal);
            
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture();
        });
    }
    
}
