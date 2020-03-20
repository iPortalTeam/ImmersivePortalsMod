package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEMatrix4f;
import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
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
        
        Quaternion cameraRotation = getCameraRotation(camera.getPitch(), camera.getYaw());
        Quaternion finalRotation = getFinalRotation(cameraRotation);
        
        matrixStack.multiply(finalRotation);
        
        CGlobal.renderer.applyAdditionalTransformations(matrixStack);
        
        //applyMirrorTransformation(camera, matrixStack);
        
    }
    
    public static Quaternion getFinalRotation(Quaternion cameraRotation) {
        double progress = (MyRenderHelper.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);

        if (progress < 0 || progress >= 1) {
            return cameraRotation;
        }

//        if (inertialRotation != null) {
//
//            if (Helper.isClose(inertialRotation, cameraRotation, 0.000001f)) {
//                inertialRotation = null;
//                return cameraRotation;
//            }
//
//            inertialRotation = Helper.interpolateQuaternion(
//                inertialRotation, cameraRotation, 0.04f
//            );
//            return inertialRotation;
//        }
//        else {
//            return cameraRotation;
//        }
        
        progress = mapProgress(progress);

        return Helper.interpolateQuaternion(
            inertialRotation, Helper.ortholize(cameraRotation.copy()), (float) progress
        );
    }
    
    private static double mapProgress(double progress) {
//        return progress;
        return Math.sin(progress * (Math.PI / 2));
//        return Math.sqrt(1 - (1 - progress) * (1 - progress));
    }
    
    public static Quaternion getCameraRotation(float pitch, float yaw) {
        Quaternion cameraRotation = Vector3f.POSITIVE_X.getDegreesQuaternion(pitch);
        cameraRotation.hamiltonProduct(
            Vector3f.POSITIVE_Y.getDegreesQuaternion(yaw + 180.0F)
        );
        return cameraRotation;
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
                getFinalRotation(getCameraRotation(player.pitch, player.yaw));
            
            Quaternion visualRotation =
                currentCameraRotation.copy();
            Quaternion b = portal.rotation.copy();
            b.conjugate();
            visualRotation.hamiltonProduct(b);
            
            Vec3d oldViewVector = player.getRotationVec(MyRenderHelper.partialTicks);
            Vec3d newViewVector = portal.transformLocalVec(oldViewVector);
            
            player.yaw = getYawFromViewVector(newViewVector);
            player.prevYaw = player.yaw;
            player.pitch = getPitchFromViewVector(newViewVector);
            player.prevPitch = player.pitch;
            
            Quaternion newCameraRotation = getCameraRotation(player.pitch, player.yaw);
            
            if (!Helper.isClose(newCameraRotation, visualRotation, 0.001f)) {
                inertialRotation = visualRotation;
                interpolationStartTime = MyRenderHelper.renderStartNanoTime;
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
            MyRenderHelper.partialTicks
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
}
