package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

@Environment(EnvType.CLIENT)
public class TransformationManager {
    
    private static DQuaternion interpolationStart;
    private static DQuaternion lastCameraRotation;
    
    private static long interpolationStartTime = 0;
    private static long interpolationEndTime = 1;
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public static boolean isIsometricView = false;
    public static float isometricViewLength = 50;
    
    private static DQuaternion getNormalCameraRotation(
        Direction gravityDirection,
        float pitch, float yaw
    ) {
        DQuaternion extra = GravityChangerInterface.invoker.getExtraCameraRotation(gravityDirection);
        DQuaternion cameraRotation = DQuaternion.getCameraRotation(pitch, yaw);
        if (extra == null) {
            return cameraRotation;
        }
        else {
            return cameraRotation.hamiltonProduct(extra);
        }
    }
    
    public static void processTransformation(Camera camera, PoseStack matrixStack) {
//        if (!WorldRenderInfo.isRendering()) {
//            ((IECamera) camera).portal_setPos(RenderStates.viewBobbedCameraPos);
//        }
        
        if (isAnimationRunning()) {
            // override vanilla camera transformation
            matrixStack.last().pose().identity();
            matrixStack.last().normal().identity();
            
            Direction gravityDir = GravityChangerInterface.invoker.getGravityDirection(client.player);
            
            DQuaternion cameraRotation = getNormalCameraRotation(gravityDir, camera.getXRot(), camera.getYRot());
            
            DQuaternion finalRotation = getAnimatedCameraRotation(cameraRotation);
            
            matrixStack.mulPose(finalRotation.toMcQuaternion());
        }
        
        WorldRenderInfo.applyAdditionalTransformations(matrixStack);
        
    }
    
    public static boolean isAnimationRunning() {
        if (interpolationStartTime == 0) {
            return false;
        }
        
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        return progress >= -0.1 && progress <= 1.1;
    }
    
    public static DQuaternion getAnimatedCameraRotation(DQuaternion cameraRotation) {
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        if (progress < 0 || progress >= 1) {
            return cameraRotation;
        }
        
        progress = mapProgress(progress);
        
        // adjust the interpolation start
        DQuaternion cameraRotDelta = cameraRotation.hamiltonProduct(lastCameraRotation.getConjugated());
        interpolationStart = interpolationStart.hamiltonProduct(cameraRotDelta);
        
        lastCameraRotation = cameraRotation;
        
        return DQuaternion.interpolate(
            interpolationStart,
            cameraRotation,
            progress
        );
    }
    
    public static double mapProgress(double progress) {
//        return progress;
        return Math.sin(progress * (Math.PI / 2));
//        return Math.sqrt(1 - (1 - progress) * (1 - progress));
    }
    
    // this may change player velocity, must change it back
    public static void managePlayerRotationAndChangeGravity(
        Portal portal
    ) {
        if (portal.getRotation() != null) {
            LocalPlayer player = client.player;
            
            Direction oldGravityDir = GravityChangerInterface.invoker.getGravityDirection(player);
            
            DQuaternion oldCameraRotation = getNormalCameraRotation(
                oldGravityDir,
                player.getViewXRot(RenderStates.tickDelta), player.getViewYRot(RenderStates.tickDelta)
            );
            DQuaternion currentCameraRotationInterpolated = getAnimatedCameraRotation(oldCameraRotation);
            
            DQuaternion cameraRotationThroughPortal =
                currentCameraRotationInterpolated.hamiltonProduct(
                    portal.getRotation().getConjugated()
                );
            
            Direction newGravityDir = portal.getTeleportChangesGravity() ?
                portal.getTransformedGravityDirection(oldGravityDir) : oldGravityDir;
            
            if (newGravityDir != oldGravityDir) {
                GravityChangerInterface.invoker.setClientPlayerGravityDirection(
                    player, newGravityDir
                );
            }
            
            DQuaternion newExtraCameraRot = GravityChangerInterface.invoker.getExtraCameraRotation(newGravityDir);
            
            DQuaternion newCameraRotationWithNormalGravity;
            if (newExtraCameraRot != null) {
                newCameraRotationWithNormalGravity =
                    (cameraRotationThroughPortal).hamiltonProduct(newExtraCameraRot.getConjugated());
            }
            else {
                newCameraRotationWithNormalGravity = cameraRotationThroughPortal;
            }
            
            Tuple<Double, Double> pitchYaw =
                DQuaternion.getPitchYawFromRotation(newCameraRotationWithNormalGravity);
            
            float finalYaw = (float) (double) (pitchYaw.getB());
            float finalPitch = (float) (double) (pitchYaw.getA());
            
            if (finalPitch > 90) {
                finalPitch = 90 - (finalPitch - 90);
            }
            else if (finalPitch < -90) {
                finalPitch = -90 + (-90 - finalPitch);
            }
            
            player.setYRot(finalYaw);
            player.setXRot(finalPitch);
            
            player.yRotO = finalYaw;
            player.xRotO = finalPitch;
            player.yBob = finalYaw;
            player.xBob = finalPitch;
            player.yBobO = finalYaw;
            player.xBobO = finalPitch;
            
            DQuaternion newCameraRotation = getNormalCameraRotation(newGravityDir, finalPitch, finalYaw);
            
            if (!DQuaternion.isClose(newCameraRotation, cameraRotationThroughPortal, 0.001f)) {
                interpolationStart = cameraRotationThroughPortal;
                lastCameraRotation = newCameraRotation;
                interpolationStartTime = RenderStates.renderStartNanoTime;
                interpolationEndTime = interpolationStartTime +
                    Helper.secondToNano(getAnimationDurationSeconds());
            }
            
            updateCamera(client);
        }
    }
    
    private static double getAnimationDurationSeconds() {
        return 1;
    }
    
    private static void updateCamera(Minecraft client) {
        Camera camera = client.gameRenderer.getMainCamera();
        camera.setup(
            client.level,
            client.player,
            !client.options.getCameraType().isFirstPerson(),
            client.options.getCameraType().isMirrored(),
            RenderStates.tickDelta
        );
    }
    
    public static Matrix4f getMirrorTransformation(Vec3 normal) {
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
        float[] arr = new float[]{
            1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
            0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
            0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
            0, 0, 0, 1
        };
        Matrix4f matrix = new Matrix4f();
        matrix.set(arr);
        return matrix;
    }
    
    // https://docs.microsoft.com/en-us/windows/win32/opengl/glortho
    public static Matrix4f getIsometricProjection() {
        int w = client.getWindow().getWidth();
        int h = client.getWindow().getHeight();
        
        float wView = (isometricViewLength / h) * w;
        
        float near = -2000;
        float far = 2000;
        
        float left = -wView / 2;
        float right = wView / 2;
        
        float top = isometricViewLength / 2;
        float bottom = -isometricViewLength / 2;
        
        float[] arr = new float[]{
            2.0f / (right - left), 0, 0, -(right + left) / (right - left),
            0, 2.0f / (top - bottom), 0, -(top + bottom) / (top - bottom),
            0, 0, -2.0f / (far - near), -(far + near) / (far - near),
            0, 0, 0, 1
        };
        Matrix4f m1 = new Matrix4f();
        m1.set(arr);
        
        return m1;
    }
    
    public static boolean isCalculatingViewBobbingOffset = false;
    
    @Environment(EnvType.CLIENT)
    public static class RemoteCallables {
        public static void enableIsometricView(float viewLength) {
            isometricViewLength = viewLength;
            isIsometricView = true;
            
            client.smartCull = false;
        }
        
        public static void disableIsometricView() {
            isIsometricView = false;
            
            client.smartCull = true;
        }
    }
    
    // isometric is equivalent to the camera being in infinitely far place
    public static Vec3 getIsometricAdjustedCameraPos() {
        Camera camera = client.gameRenderer.getMainCamera();
        return getIsometricAdjustedCameraPos(camera);
    }
    
    public static Vec3 getIsometricAdjustedCameraPos(Camera camera) {
        Vec3 cameraPos = camera.getPosition();
        
        if (!isIsometricView) {
            return cameraPos;
        }
        
        Quaternionf rotation = camera.rotation();
        Vector3f vec = new Vector3f(0, 0, client.options.getEffectiveRenderDistance() * -10);
        rotation.transform(vec);
        
        return cameraPos.add(vec.x(), vec.y(), vec.z());
    }
}
