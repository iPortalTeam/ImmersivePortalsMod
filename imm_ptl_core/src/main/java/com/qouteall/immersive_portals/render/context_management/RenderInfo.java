package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.ducks.IECamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

public class RenderInfo {
    public final ClientWorld world;
    public final Vec3d cameraPos;
    @Nullable
    public final Matrix4f additionalTransformation;
    @Nullable
    public final UUID description;
    public final int renderDistance;
    
    private static final Stack<RenderInfo> renderInfoStack = new Stack<>();
    
    public RenderInfo(
        ClientWorld world, Vec3d cameraPos,
        Matrix4f additionalTransformation, @Nullable UUID description
    ) {
        this(
            world, cameraPos, additionalTransformation, description,
            MinecraftClient.getInstance().options.viewDistance
        );
    }
    
    public RenderInfo(
        ClientWorld world, Vec3d cameraPos,
        @Nullable Matrix4f additionalTransformation,
        @Nullable UUID description, int renderDistance
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.additionalTransformation = additionalTransformation;
        this.description = description;
        this.renderDistance = renderDistance;
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
    
    /**
     * it's different from {@link PortalRendering#isRendering()}
     * when rendering cross portal third person view, this is true
     * but {@link PortalRendering#isRendering()} is false
     */
    public static boolean isRendering() {
        return !renderInfoStack.empty();
    }
    
    public static int getRenderingLayer() {
        return renderInfoStack.size();
    }
    
    // for example rendering portal B inside portal A will always have the same rendering description
    public static List<UUID> getRenderingDescription() {
        return renderInfoStack.stream()
            .map(renderInfo -> renderInfo.description).collect(Collectors.toList());
    }
}
