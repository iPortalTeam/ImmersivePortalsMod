package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vector4f;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ducks.IEShader;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class FrontClipping {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static double[] activeClipPlaneEquation;
    
    // entity rendering transforms vertices by modelView on CPU
    private static double[] activeClipPlaneForEntities;
    
    public static boolean isClippingEnabled = false;
    
    public static void disableClipping() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
        isClippingEnabled = false;
    }
    
    private static void enableClipping() {
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        isClippingEnabled = true;
    }
    
    public static void updateInnerClipping(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            setupInnerClipping(PortalRendering.getRenderingPortal(), false, matrixStack);
        }
        else {
            disableClipping();
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public static void setupInnerClipping(
        PortalLike portalLike, boolean doCompensate, MatrixStack matrixStack
    ) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        
        final Plane clipping = portalLike.getInnerClipping();
        
        if (clipping != null) {
            activeClipPlaneEquation = getClipEquationInner(doCompensate, clipping.pos, clipping.normal);
            activeClipPlaneForEntities = transformClipEquation(activeClipPlaneEquation, matrixStack);
            
            enableClipping();
        }
        else {
            activeClipPlaneEquation = null;
            disableClipping();
        }
    }
    
    private static double[] transformClipEquation(
        double[] equation, MatrixStack matrixStack
    ) {
        Vector4f eq =
            new Vector4f((float) equation[0], (float) equation[1], (float) equation[2], (float) equation[3]);
        Matrix4f m = matrixStack.peek().getModel().copy();
        m.invert();
        m.transpose();
        eq.transform(m);
        return new double[]{eq.getX(), eq.getY(), eq.getZ(), eq.getW()};
    }
    
    private static double[] getClipEquationInner(
        boolean doCompensate, Vec3d clippingPoint, Vec3d clippingDirection
    ) {
        
        Vec3d cameraPos = CHelper.getCurrentCameraPos();
        
        
        Vec3d planeNormal = clippingDirection;
        
        double correction;
        
        if (doCompensate) {
            correction = clippingPoint.subtract(cameraPos)
                .dotProduct(clippingDirection) / 150.0;
        }
        else {
            correction = 0;
        }
        
        Vec3d portalPos = clippingPoint
            .subtract(planeNormal.multiply(correction))//avoid z fighting
            .subtract(cameraPos);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static void setupOuterClipping(MatrixStack matrixStack, PortalLike portalLike) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        
        if (portalLike instanceof Portal) {
            activeClipPlaneEquation = getClipEquationOuter(((Portal) portalLike));
            activeClipPlaneForEntities = transformClipEquation(activeClipPlaneEquation, matrixStack);
            enableClipping();
        }
        else {
            activeClipPlaneEquation = null;
            disableClipping();
        }
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
    
    public static double[] getActiveClipPlaneEquationForEntities() {
        return activeClipPlaneForEntities;
    }
    
    public static void updateClippingEquationUniformForCurrentShader(boolean isRenderingEntities) {
        Shader shader = RenderSystem.getShader();
        
        if (shader == null) {
            return;
        }
        
        GlUniform clippingEquationUniform = ((IEShader) shader).ip_getClippingEquationUniform();
        if (clippingEquationUniform != null) {
            if (isClippingEnabled) {
                double[] equation = isRenderingEntities ? activeClipPlaneForEntities : activeClipPlaneEquation;
                clippingEquationUniform.set(
                    (float) equation[0],
                    (float) equation[1],
                    (float) equation[2],
                    (float) equation[3]
                );
            }
            else {
                clippingEquationUniform.set(0f, 0f, 0f, 1f);
            }
        }
    }
}
