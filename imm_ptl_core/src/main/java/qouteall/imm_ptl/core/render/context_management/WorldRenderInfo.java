package qouteall.imm_ptl.core.render.context_management;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.ducks.IECamera;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A world rendering task.
 */
public class WorldRenderInfo {
    
    /**
     * The dimension that it's going to render
     */
    public final ClientLevel world;
    
    /**
     * Camera position
     */
    public final Vec3 cameraPos;
    
    public final boolean overwriteCameraTransformation;
    
    /**
     * If overwriteCameraTransformation is true,
     * the world rendering camera transformation will be replaced by this.
     * If overwriteCameraTransformation is false,
     * this will be applied to the original camera transformation, and this can be null
     */
    @Nullable
    public final Matrix4f cameraTransformation;
    
    /**
     * Used for visibility prediction optimization
     */
    @Nullable
    public final UUID description;
    
    /**
     * Render distance.
     * It cannot render the chunks that are not synced to client.
     */
    public final int renderDistance;
    
    public final boolean doRenderHand;
    
    public final boolean enableViewBobbing;
    
    private static final Stack<WorldRenderInfo> renderInfoStack = new Stack<>();
    
    // should use the builder or the full constructor
    @Deprecated
    public WorldRenderInfo(
        ClientLevel world, Vec3 cameraPos,
        @Nullable Matrix4f cameraTransformation,
        boolean overwriteCameraTransformation,
        @Nullable UUID description,
        int renderDistance
    ) {
        this(
            world, cameraPos, cameraTransformation, overwriteCameraTransformation,
            description, renderDistance, false
        );
    }
    
    // should use the builder or the full constructor
    @Deprecated
    public WorldRenderInfo(
        ClientLevel world, Vec3 cameraPos,
        @Nullable Matrix4f cameraTransformation,
        boolean overwriteCameraTransformation,
        @Nullable UUID description,
        int renderDistance,
        boolean doRenderHand
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.cameraTransformation = cameraTransformation;
        this.description = description;
        this.renderDistance = renderDistance;
        this.overwriteCameraTransformation = overwriteCameraTransformation;
        this.doRenderHand = doRenderHand;
        this.enableViewBobbing = true;
    }
    
    public WorldRenderInfo(
        ClientLevel world, Vec3 cameraPos,
        @Nullable Matrix4f cameraTransformation,
        boolean overwriteCameraTransformation,
        @Nullable UUID description,
        int renderDistance,
        boolean doRenderHand,
        boolean enableViewBobbing
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.cameraTransformation = cameraTransformation;
        this.description = description;
        this.renderDistance = renderDistance;
        this.overwriteCameraTransformation = overwriteCameraTransformation;
        this.doRenderHand = doRenderHand;
        this.enableViewBobbing = enableViewBobbing;
    }
    
    public static void pushRenderInfo(WorldRenderInfo worldRenderInfo) {
        renderInfoStack.push(worldRenderInfo);
    }
    
    public static void popRenderInfo() {
        renderInfoStack.pop();
    }
    
    public static void adjustCameraPos(Camera camera) {
        if (!renderInfoStack.isEmpty()) {
            WorldRenderInfo currWorldRenderInfo = renderInfoStack.peek();
            ((IECamera) camera).portal_setPos(currWorldRenderInfo.cameraPos);
        }
    }
    
    public static void applyAdditionalTransformations(PoseStack matrixStack) {
        for (WorldRenderInfo worldRenderInfo : renderInfoStack) {
            if (worldRenderInfo.overwriteCameraTransformation) {
                matrixStack.last().pose().identity();
                matrixStack.last().normal().identity();
            }
            
            Matrix4f matrix = worldRenderInfo.cameraTransformation;
            if (matrix != null) {
                matrixStack.last().pose().mul(matrix);
                
                Matrix3f normalMatrixMult = new Matrix3f(matrix);
                // make its determinant 1, so it won't scale the normal vector
                normalMatrixMult.scale(
                    (float) Math.pow(1.0 / Math.abs(normalMatrixMult.determinant()), 1.0 / 3)
                );
                matrixStack.last().normal().mul(normalMatrixMult);
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
    
    public static int getRenderDistance() {
        if (renderInfoStack.isEmpty()) {
            return Minecraft.getInstance().options.getEffectiveRenderDistance();
        }
        
        return renderInfoStack.peek().renderDistance;
    }
    
    public static Vec3 getCameraPos() {
        Validate.isTrue(!renderInfoStack.isEmpty());
        return renderInfoStack.peek().cameraPos;
    }
    
    public static boolean isViewBobbingEnabled() {
        return renderInfoStack.stream().allMatch(info -> info.enableViewBobbing);
    }
    
    public static class Builder {
        private ClientLevel world;
        private Vec3 cameraPos;
        private Matrix4f cameraTransformation;
        private boolean overwriteCameraTransformation;
        private UUID description;
        private int renderDistance;
        private boolean doRenderHand;
        private boolean enableViewBobbing;
        
        public Builder() {
            this.cameraTransformation = null;
            this.overwriteCameraTransformation = false;
            this.description = null;
            this.renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
            this.doRenderHand = false;
            this.enableViewBobbing = true;
        }
        
        public Builder setWorld(ClientLevel world) {
            this.world = world;
            return this;
        }
        
        public Builder setCameraPos(Vec3 cameraPos) {
            this.cameraPos = cameraPos;
            return this;
        }
        
        public Builder setCameraTransformation(Matrix4f cameraTransformation) {
            this.cameraTransformation = cameraTransformation;
            return this;
        }
        
        public Builder setOverwriteCameraTransformation(boolean overwriteCameraTransformation) {
            this.overwriteCameraTransformation = overwriteCameraTransformation;
            return this;
        }
        
        public Builder setDescription(UUID description) {
            this.description = description;
            return this;
        }
        
        public Builder setRenderDistance(int renderDistance) {
            this.renderDistance = renderDistance;
            return this;
        }
        
        public Builder setDoRenderHand(boolean doRenderHand) {
            this.doRenderHand = doRenderHand;
            return this;
        }
        
        public Builder setEnableViewBobbing(boolean enableViewBobbing) {
            this.enableViewBobbing = enableViewBobbing;
            return this;
        }
        
        public WorldRenderInfo build() {
            Validate.notNull(world);
            Validate.notNull(cameraPos);
            return new WorldRenderInfo(
                world, cameraPos, cameraTransformation, overwriteCameraTransformation,
                description, renderDistance, doRenderHand, enableViewBobbing
            );
        }
    }
}
