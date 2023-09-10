package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEShader;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.Plane;

public class FrontClipping {
    private static final Minecraft client = Minecraft.getInstance();
    private static double[] activeClipPlaneEquationBeforeModelView;
    private static double[] activeClipPlaneAfterModelView;
    
    public static boolean isClippingEnabled = false;
    
    public static final double ADJUSTMENT = 0.01;
    
    public static void disableClipping() {
        if (IPGlobal.enableClippingMechanism) {
            if (isClippingEnabled) {
                GL11.glDisable(GL11.GL_CLIP_PLANE0);
                isClippingEnabled = false;
            }
        }
    }
    
    private static void enableClipping() {
        if (IPGlobal.enableClippingMechanism) {
            if (!isClippingEnabled) {
                GL11.glEnable(GL11.GL_CLIP_PLANE0);
                isClippingEnabled = true;
            }
        }
    }
    
    public static void updateInnerClipping(PoseStack matrixStack) {
        if (PortalRendering.isRendering()) {
            setupInnerClipping(
                PortalRendering.getActiveClippingPlane(),
                matrixStack.last().pose(), 0
            );
        }
        else {
            disableClipping();
        }
    }
    
    // NOTE the actual clipping plane is related to current model view matrix
    public static void setupInnerClipping(
        Plane clipping, Matrix4f modelView, double adjustment
    ) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        
        // Note: the normal of plane points to the non-clipped side
        
        if (clipping != null) {
            activeClipPlaneEquationBeforeModelView =
                getClipEquationInner(clipping.pos(), clipping.normal(), adjustment);
            activeClipPlaneAfterModelView =
                transformClipEquation(activeClipPlaneEquationBeforeModelView, modelView);
            
            enableClipping();
        }
        else {
            activeClipPlaneEquationBeforeModelView = null;
            disableClipping();
        }
    }
    
    private static double[] transformClipEquation(
        double[] equation, Matrix4f modelView
    ) {
        Vector4f eq =
            new Vector4f((float) equation[0], (float) equation[1], (float) equation[2], (float) equation[3]);
        Matrix4f m = new Matrix4f(modelView);
        m.invert();
        m.transpose();
        m.transform(eq);
        return new double[]{eq.x(), eq.y(), eq.z(), eq.w()};
    }
    
    private static double[] getClipEquationInner(
        Vec3 clippingPoint, Vec3 clippingDirection, double correction
    ) {
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        
        Vec3 planeNormal = clippingDirection;
        
        Vec3 portalPos = clippingPoint
            .add(planeNormal.scale(correction))
            .subtract(cameraPos);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dot(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static void setupOuterClipping(PoseStack matrixStack, PortalLike portalLike) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        
        if (portalLike instanceof Portal) {
            activeClipPlaneEquationBeforeModelView = getClipEquationOuter(((Portal) portalLike));
            activeClipPlaneAfterModelView = transformClipEquation(activeClipPlaneEquationBeforeModelView, matrixStack.last().pose());
            enableClipping();
        }
        else {
            activeClipPlaneEquationBeforeModelView = null;
            disableClipping();
        }
    }
    
    private static double[] getClipEquationOuter(Portal portal) {
        Vec3 planeNormal = portal.getNormal();
        
        Vec3 portalPos = portal.getOriginPos()
            //.subtract(planeNormal.multiply(0.01))//avoid z fighting
            .subtract(client.gameRenderer.getMainCamera().getPosition());
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dot(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static double[] getActiveClipPlaneEquationBeforeModelView() {
        return activeClipPlaneEquationBeforeModelView;
    }
    
    public static double[] getActiveClipPlaneEquationAfterModelView() {
        return activeClipPlaneAfterModelView;
    }
    
    public static void updateClippingEquationUniformForCurrentShader(boolean isRenderingEntities) {
        if (!IPGlobal.enableClippingMechanism) {
            return;
        }
        
        ShaderInstance shader = RenderSystem.getShader();
        
        if (shader == null) {
            return;
        }
        
        Uniform clippingEquationUniform = ((IEShader) shader).ip_getClippingEquationUniform();
        if (clippingEquationUniform != null) {
            if (isClippingEnabled) {
                double[] equation = isRenderingEntities ? activeClipPlaneAfterModelView : activeClipPlaneEquationBeforeModelView;
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
    
    public static void unsetClippingUniform() {
        if (!IPGlobal.enableClippingMechanism) {
            return;
        }
        
        ShaderInstance shader = RenderSystem.getShader();
        
        if (shader == null) {
            return;
        }
        
        Uniform clippingEquationUniform = ((IEShader) shader).ip_getClippingEquationUniform();
        if (clippingEquationUniform != null) {
            clippingEquationUniform.set(0f, 0f, 0f, 1f);
        }
    }
}
