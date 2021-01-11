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

public class WorldRendering {
    public final ClientWorld world;
    public final Vec3d cameraPos;
    @Nullable
    public final Matrix4f cameraTransformation;
    @Nullable
    public final UUID description;
    public final int renderDistance;
    public final boolean overwriteCameraTransformation;
    
    private static final Stack<WorldRendering> renderInfoStack = new Stack<>();
    
    public WorldRendering(
        ClientWorld world, Vec3d cameraPos,
        Matrix4f cameraTransformation, @Nullable UUID description
    ) {
        this(
            world, cameraPos, cameraTransformation, description,
            MinecraftClient.getInstance().options.viewDistance,
            false
        );
    }
    
    public WorldRendering(
        ClientWorld world, Vec3d cameraPos,
        @Nullable Matrix4f cameraTransformation,
        @Nullable UUID description, int renderDistance,
        boolean overwriteCameraTransformation
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.cameraTransformation = cameraTransformation;
        this.description = description;
        this.renderDistance = renderDistance;
        this.overwriteCameraTransformation = overwriteCameraTransformation;
    }
    
    public static void pushRenderInfo(WorldRendering worldRendering) {
        renderInfoStack.push(worldRendering);
    }
    
    public static void popRenderInfo() {
        renderInfoStack.pop();
    }
    
    public static void adjustCameraPos(Camera camera) {
        if (!renderInfoStack.isEmpty()) {
            WorldRendering currWorldRendering = renderInfoStack.peek();
            ((IECamera) camera).portal_setPos(currWorldRendering.cameraPos);
        }
    }
    
    public static void applyAdditionalTransformations(MatrixStack matrixStack) {
        for (WorldRendering worldRendering : renderInfoStack) {
            if (worldRendering.overwriteCameraTransformation) {
                matrixStack.peek().getModel().loadIdentity();
                matrixStack.peek().getNormal().loadIdentity();
            }
            
            Matrix4f matrix = worldRendering.cameraTransformation;
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
