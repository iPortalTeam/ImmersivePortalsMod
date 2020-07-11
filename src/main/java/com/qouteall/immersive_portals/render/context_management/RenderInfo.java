package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.Stack;

public class RenderInfo {
    public ClientWorld world;
    public Vec3d cameraPos;
    @Nullable
    public Matrix4f additionalTransformation;
    @Nullable
    public Portal portal;
    
    private static final Stack<RenderInfo> renderInfoStack = new Stack<>();
    
    public RenderInfo(
        ClientWorld world, Vec3d cameraPos,
        Matrix4f additionalTransformation, @Nullable Portal portal
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.additionalTransformation = additionalTransformation;
        this.portal = portal;
    }
    
    public static void pushRenderInfo(RenderInfo renderInfo) {
        renderInfoStack.push(renderInfo);
    }
    
    public static void popRenderInfo() {
        renderInfoStack.pop();
    }
    
    public static void adjustCameraPos(Camera camera) {
        if (!renderInfoStack.isEmpty()) {
            RenderInfo currRenderInfo = renderInfoStack.peek();
            ((IECamera) camera).portal_setPos(currRenderInfo.cameraPos);
        }
    }
    
    public static void applyAdditionalTransformations(MatrixStack matrixStack) {
        for (RenderInfo renderInfo : renderInfoStack) {
            Matrix4f matrix = renderInfo.additionalTransformation;
            if (matrix != null) {
                matrixStack.peek().getModel().multiply(matrix);
                matrixStack.peek().getNormal().multiply(new Matrix3f(matrix));
            }
        }
    }
}
