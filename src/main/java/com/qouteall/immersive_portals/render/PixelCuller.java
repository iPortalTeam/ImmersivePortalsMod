package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.optifine_compatibility.ShaderCullingManager;
import com.qouteall.immersive_portals.portal.Portal;
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
        if (OFInterface.isShaders.getAsBoolean()) {
            ShaderCullingManager.update();
        }
    }
    
    public static void startCulling() {
        //shaders do not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !OFInterface.isShaders.getAsBoolean()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
        isCullingEnabled = true;
        if (OFInterface.isShaders.getAsBoolean()) {
            ShaderCullingManager.update();
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public static void updateCullingPlaneInner(MatrixStack matrixStack, Portal portal) {
        activeClipPlaneEquation = getClipEquationInner(portal);
        if (!OFInterface.isShaders.getAsBoolean()) {
            McHelper.runWithTransformation(
                matrixStack,
                () -> {
                    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, activeClipPlaneEquation);
                }
            );
        }
        else {
            ShaderCullingManager.update();
        }
    }
    
    public static void updateCullingPlaneOuter(MatrixStack matrixStack, Portal portal) {
        activeClipPlaneEquation = getClipEquationOuter(portal);
        if (!OFInterface.isShaders.getAsBoolean()) {
            McHelper.runWithTransformation(
                matrixStack,
                () -> {
                    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, activeClipPlaneEquation);
                }
            );
        }
        else {
            ShaderCullingManager.update();
        }
    }
    
    //invoke this before rendering portal
    //its result depends on camera pos
    private static double[] getClipEquationInner(Portal portal) {
        
        Vec3d planeNormal = portal.getContentDirection();
        
        Vec3d portalPos = portal.destination
            .subtract(planeNormal.multiply(0.01))//avoid z fighting
            .subtract(client.gameRenderer.getCamera().getPos());
        
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
