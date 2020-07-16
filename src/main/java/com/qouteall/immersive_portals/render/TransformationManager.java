package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEMatrix4f;
import com.qouteall.immersive_portals.my_util.RotationHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class TransformationManager {
    public static Quaternion inertialRotation;
    private static long interpolationStartTime = 0;
    private static long interpolationEndTime = 1;
    public static Quaternion portalRotation;
    
    public static void processTransformation(Camera camera, MatrixStack matrixStack) {
        matrixStack.peek().getModel().loadIdentity();
        matrixStack.peek().getNormal().loadIdentity();
        
        Quaternion cameraRotation = RotationHelper.getCameraRotation(camera.getPitch(), camera.getYaw());
        Quaternion finalRotation = getFinalRotation(cameraRotation);
        
        matrixStack.multiply(finalRotation);
        
        RenderInfo.applyAdditionalTransformations(matrixStack);
        
    }
    
    public static boolean isAnimationRunning() {
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        return progress >= -0.1 && progress <= 1.1;
    }
    
    public static Quaternion getFinalRotation(Quaternion cameraRotation) {
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        if (progress < 0 || progress >= 1) {
            return cameraRotation;
        }
        
        progress = mapProgress(progress);
        
        return RotationHelper.interpolateQuaternion(
            RotationHelper.ortholize(inertialRotation),
            RotationHelper.ortholize(cameraRotation.copy()),
            (float) progress
        );
    }
    
    private static double mapProgress(double progress) {
//        return progress;
        return Math.sin(progress * (Math.PI / 2));
//        return Math.sqrt(1 - (1 - progress) * (1 - progress));
    }
    
    /**
     * Entity#getRotationVector(float, float)
     */
    private static float getYawFromViewVector(Vec3d viewVector) {
        double lx = viewVector.x;
        double lz = viewVector.z;
        double len = Math.sqrt(lx * lx + lz * lz);
        lx /= len;
        lz /= len;
        
        if (lz >= 0) {
            return (float) -Math.asin(lx) / 0.017453292F;
        }
        else {
            if (lx > 0) {
                return (float) -Math.acos(lz) / 0.017453292F;
            }
            else {
                return (float) Math.acos(lz) / 0.017453292F;
            }
        }
    }
    
    private static float getPitchFromViewVector(Vec3d viewVector) {
        return (float) -Math.asin(viewVector.y) / 0.017453292F;
    }
    
    public static void onClientPlayerTeleported(
        Portal portal
    ) {
        if (portal.rotation != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            
            Quaternion currentCameraRotation =
                getFinalRotation(RotationHelper.getCameraRotation(player.pitch, player.yaw));
            
            Quaternion visualRotation =
                currentCameraRotation.copy();
            Quaternion b = portal.rotation.copy();
            b.conjugate();
            visualRotation.hamiltonProduct(b);
            visualRotation.normalize();
            
            Vec3d oldViewVector = player.getRotationVec(RenderStates.tickDelta);
            Vec3d newViewVector;
            
            Pair<Double, Double> pitchYaw = RotationHelper.getPitchYawFromRotation(visualRotation);
            
            player.yaw = (float) Math.toDegrees(pitchYaw.getRight());
            player.pitch = (float) Math.toDegrees(pitchYaw.getLeft());
            
            if (player.pitch > 90) {
                player.pitch = 90 - (player.pitch - 90);
            }
            else if (player.pitch < -90) {
                player.pitch = -90 + (-90 - player.pitch);
            }
            
            player.prevYaw = player.yaw;
            player.prevPitch = player.pitch;
            player.renderYaw = player.yaw;
            player.renderPitch = player.pitch;
            player.lastRenderYaw = player.renderYaw;
            player.lastRenderPitch = player.renderPitch;
            
            Quaternion newCameraRotation = RotationHelper.getCameraRotation(player.pitch, player.yaw);
            
            if (!RotationHelper.isClose(newCameraRotation, visualRotation, 0.001f)) {
                inertialRotation = visualRotation;
                interpolationStartTime = RenderStates.renderStartNanoTime;
                interpolationEndTime = interpolationStartTime +
                    Helper.secondToNano(1);
            }
            
            updateCamera(client);
        }
    }
    
    private static void updateCamera(MinecraftClient client) {
        Camera camera = client.gameRenderer.getCamera();
        camera.update(
            client.world,
            client.player,
            client.options.perspective > 0,
            client.options.perspective == 2,
            RenderStates.tickDelta
        );
    }
    
    public static Matrix4f getMirrorTransformation(Vec3d normal) {
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
        float[] arr =
            new float[]{
                1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
                0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
                0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
                0, 0, 0, 1
            };
        Matrix4f matrix = new Matrix4f();
        ((IEMatrix4f) (Object) matrix).loadFromArray(arr);
        return matrix;
    }

//    private static void www(){
//        float x1 = this.getX();
//        float y1 = this.getY();
//        float z1 = this.getZ();
//        float w1 = this.getW();
//        float x2 = other.getX();
//        float y2 = other.getY();
//        float z2 = other.getZ();
//        float w2 = other.getW();
//        this.x = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2;
//        this.y = w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2;
//        this.z = w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2;
//        this.w = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2;
//    }
}
