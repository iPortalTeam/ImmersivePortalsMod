package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class FrontClipping {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static double[] activeClipPlaneEquation;
    public static boolean isClippingEnabled = false;
    
    public static void disableClipping() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
        isClippingEnabled = false;
    }
    
    public static void enableClipping() {
        //shaders do not compatible with glClipPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !isShaderClipping()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
        isClippingEnabled = true;
    }
    
    public static void startClassicalCulling() {
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        isClippingEnabled = true;
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public static void updateClippingPlaneInner(
        MatrixStack matrixStack, Portal portal, boolean doCompensate
    ) {
        activeClipPlaneEquation = getClipEquationInner(portal, doCompensate);
        if (!isShaderClipping()) {
            loadClippingPlaneClassical(matrixStack);
        }
    }
    
    public static boolean isShaderClipping() {
        return OFInterface.isShaders.getAsBoolean() &&
            !RenderDimensionRedirect.isNoShader(
                MinecraftClient.getInstance().world.getRegistryKey()
            );
    }
    
    public static void loadClippingPlaneClassical(MatrixStack matrixStack) {
        McHelper.runWithTransformation(
            matrixStack,
            () -> {
                GL11.glClipPlane(GL11.GL_CLIP_PLANE0, activeClipPlaneEquation);
            }
        );
    }
    
    public static void updateClippingPlaneOuter(MatrixStack matrixStack, Portal portal) {
        activeClipPlaneEquation = getClipEquationOuter(portal);
        if (!isShaderClipping()) {
            loadClippingPlaneClassical(matrixStack);
        }
    }
    
    private static double[] getClipEquationInner(Portal portal, boolean doCompensate) {
        
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        
        Vec3d planeNormal = portal.getContentDirection();
        
        double correction;
        if (doCompensate) {
            correction = portal.getDestPos().subtract(cameraPos)
                .dotProduct(portal.getContentDirection()) / 150.0;
        }
        else {
            correction = 0;
        }
        
        Vec3d portalPos = portal.getDestPos()
            .subtract(planeNormal.multiply(correction))//avoid z fighting
            .subtract(cameraPos);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    private static double[] getClipEquationOuter(Portal portal) {
        Vec3d planeNormal = portal.getNormal();
    
        Vec3d portalPos = portal.getOriginPos()
            //.subtract(planeNormal.multiply(0.01))//avoid z fighting
            .subtract(client.gameRenderer.getCamera().getPos());
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static double[] getActiveClipPlaneEquation() {
        return activeClipPlaneEquation;
    }
}
