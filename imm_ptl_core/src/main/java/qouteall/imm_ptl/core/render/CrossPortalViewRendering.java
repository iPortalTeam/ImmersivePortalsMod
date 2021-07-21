package qouteall.imm_ptl.core.render;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.PehkuiInterface;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;

// in third person view it may render cross portal.
// if view bobbing make the camera go through portal before the actual player go through portal,
//  it will also render cross-portal
public class CrossPortalViewRendering {
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    // if rendered, return true
    public static boolean renderCrossPortalView() {
        if (!IPGlobal.enableCrossPortalView) {
            return false;
        }
        
        Entity cameraEntity = client.cameraEntity;
        
        Camera camera1 = new Camera();
        float cameraY = ((IECamera) RenderStates.originalCamera).getCameraY();
        float lastCameraY = ((IECamera) RenderStates.originalCamera).getLastCameraY();
        ((IECamera) camera1).setCameraY(cameraY, lastCameraY);
        Camera camera = camera1;
        camera.update(
            client.world, cameraEntity,
            isThirdPerson(),
            isFrontView(),
            RenderStates.tickDelta
        );
        
        Vec3d viewBobbingOffset = getViewBobbingOffset(camera);
        ((IECamera) camera).portal_setPos(camera.getPos().add(viewBobbingOffset));
        
        Vec3d realCameraPos = camera.getPos();
        RenderStates.viewBobbedCameraPos = realCameraPos;
        Vec3d isometricAdjustedOriginalCameraPos =
            TransformationManager.getIsometricAdjustedCameraPos(camera);
        
        Vec3d physicalPlayerHeadPos = ClientTeleportationManager.getPlayerHeadPos(RenderStates.tickDelta);
        
        Pair<Portal, Vec3d> portalHit = PortalCommand.raytracePortals(
            client.world, physicalPlayerHeadPos, isometricAdjustedOriginalCameraPos, true
        ).orElse(null);
        
        if (portalHit == null) {
            return false;
        }
        
        Portal portal = portalHit.getFirst();
        Vec3d hitPos = portalHit.getSecond();
        
        if (!portal.isInteractable()) {
            return false;
        }
        
        Vec3d renderingCameraPos;
        
        if (isThirdPerson()) {
            double distance = getThirdPersonMaxDistance();
            
            Vec3d thirdPersonPos = realCameraPos.subtract(physicalPlayerHeadPos).normalize()
                .multiply(distance).add(physicalPlayerHeadPos);
            
            renderingCameraPos = getThirdPersonCameraPos(thirdPersonPos, portal, hitPos);
        }
        else {
            renderingCameraPos = portal.transformPoint(realCameraPos);
        }
        
        ((IECamera) RenderStates.originalCamera).portal_setPos(renderingCameraPos);
        
        WorldRenderInfo worldRenderInfo = new WorldRenderInfo(
            ClientWorldLoader.getWorld(portal.dimensionTo),
            renderingCameraPos, portal.getAdditionalCameraTransformation(),
            false, null,
            MinecraftClient.getInstance().options.viewDistance,
            ((IEGameRenderer) client.gameRenderer).getDoRenderHand()
        );
        
        IPCGlobal.renderer.invokeWorldRendering(worldRenderInfo);
        
        return true;
    }
    
    private static boolean isFrontView() {
        return client.options.getPerspective().isFrontView();
    }
    
    private static boolean isThirdPerson() {
        return !client.options.getPerspective().isFirstPerson();
    }
    
    /**
     * {@link Camera#update(BlockView, Entity, boolean, boolean, float)}
     */
    private static Vec3d getThirdPersonCameraPos(Vec3d endPos, Portal portal, Vec3d startPos) {
        Vec3d rtStart = portal.transformPoint(startPos);
        Vec3d rtEnd = portal.transformPoint(endPos);
        BlockHitResult blockHitResult = portal.getDestinationWorld().raycast(
            new RaycastContext(
                rtStart,
                rtEnd,
                RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE,
                client.cameraEntity
            )
        );
        
        if (blockHitResult == null) {
            return rtStart.add(rtEnd.subtract(rtStart).normalize().multiply(
                getThirdPersonMaxDistance()
            ));
        }
        
        return blockHitResult.getPos();
    }
    
    private static double getThirdPersonMaxDistance() {
        return 4.0d * PehkuiInterface.getScale.apply(MinecraftClient.getInstance().player);
    }
    
    /**
     * {@link net.minecraft.client.render.GameRenderer#renderWorld(float, long, MatrixStack)}
     */
    private static Vec3d getViewBobbingOffset(Camera camera) {
        MatrixStack matrixStack = new MatrixStack();
        
        if (client.options.bobView) {
            if (client.getCameraEntity() instanceof PlayerEntity) {
                PlayerEntity playerEntity = (PlayerEntity) client.getCameraEntity();
                float g = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
                float h = -(playerEntity.horizontalSpeed + g * RenderStates.tickDelta);
                float i = MathHelper.lerp(
                    RenderStates.tickDelta, playerEntity.prevStrideDistance, playerEntity.strideDistance
                );
                matrixStack.translate(
                    (double) (MathHelper.sin(h * 3.1415927F) * i * 0.5F),
                    (double) (-Math.abs(MathHelper.cos(h * 3.1415927F) * i)),
                    0.0D
                );
                matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(
                    MathHelper.sin(h * 3.1415927F) * i * 3.0F
                ));
                matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(
                    Math.abs(MathHelper.cos(h * 3.1415927F - 0.2F) * i) * 5.0F
                ));
            }
        }
        
        matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(camera.getPitch()));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(camera.getYaw() + 180.0F));
        
        WorldRenderInfo.applyAdditionalTransformations(matrixStack);
        
        Matrix4f matrix = matrixStack.peek().getModel();
        matrix.invert();
        Vector4f origin = new Vector4f(0, 0, 0, 1);
        origin.transform(matrix);
        
        return new Vec3d(origin.getX(), origin.getY(), origin.getZ());
    }

//    private static Vec3d getThirdPersonCameraPos(Portal portalHit, Camera resuableCamera) {
//        return CHelper.withWorldSwitched(
//            client.cameraEntity,
//            portalHit,
//            () -> {
//                World destinationWorld = portalHit.getDestinationWorld();
//                resuableCamera.update(
//                    destinationWorld,
//                    client.cameraEntity,
//                    true,
//                    isInverseView(),
//                    RenderStates.tickDelta
//                );
//                return resuableCamera.getPos();
//            }
//        );
//    }
}
