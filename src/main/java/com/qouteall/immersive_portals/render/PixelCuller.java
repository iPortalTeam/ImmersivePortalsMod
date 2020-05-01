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

public class PixelCuller {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static double[] activeClipPlaneEquation;
    public static boolean isCullingEnabled = false;
    
    public static void endCulling() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
        isCullingEnabled = false;
    }
    
    public static void startCulling() {
        //shaders do not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !isShaderCulling()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
        isCullingEnabled = true;
    }
    
    public static void startClassicalCulling() {
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        isCullingEnabled = true;
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public static void updateCullingPlaneInner(
        MatrixStack matrixStack, Portal portal, boolean doCompensate
    ) {
        activeClipPlaneEquation = getClipEquationInner(portal, doCompensate);
        if (!isShaderCulling()) {
            loadCullingPlaneClassical(matrixStack);
        }
    }
    
    public static boolean isShaderCulling() {
        return OFInterface.isShaders.getAsBoolean() &&
            !RenderDimensionRedirect.isNoShader(
                MinecraftClient.getInstance().world.dimension.getType()
            );
    }
    
    public static void loadCullingPlaneClassical(MatrixStack matrixStack) {
        McHelper.runWithTransformation(
            matrixStack,
            () -> {
                GL11.glClipPlane(GL11.GL_CLIP_PLANE0, activeClipPlaneEquation);
            }
        );
    }
    
    public static void updateCullingPlaneOuter(MatrixStack matrixStack, Portal portal) {
        activeClipPlaneEquation = getClipEquationOuter(portal);
        if (!isShaderCulling()) {
            loadCullingPlaneClassical(matrixStack);
        }
    }
    
    private static double[] getClipEquationInner(Portal portal, boolean doCompensate) {
        
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        
        Vec3d planeNormal = portal.getContentDirection();
        
        double correction;
        if (doCompensate) {
            correction = portal.destination.subtract(cameraPos)
                .dotProduct(portal.getContentDirection()) / 150.0;
        }
        else {
            correction = 0;
        }
        
        Vec3d portalPos = portal.destination
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
        
        Vec3d portalPos = portal.getPos()
            //.subtract(planeNormal.multiply(0.01))//avoid z fighting
            .subtract(client.gameRenderer.getCamera().getPos());
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static double[] getActiveCullingPlaneEquation() {
        return activeClipPlaneEquation;
    }
}
