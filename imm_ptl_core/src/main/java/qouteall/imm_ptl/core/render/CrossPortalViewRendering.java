package qouteall.imm_ptl.core.render;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;

// in third person view it may render cross portal.
// if view bobbing make the camera go through portal before the actual player go through portal,
//  it will also render cross-portal
public class CrossPortalViewRendering {
    public static final Minecraft client = Minecraft.getInstance();
    
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
        camera.setup(
            client.level, cameraEntity,
            isThirdPerson(),
            isFrontView(),
            RenderStates.tickDelta
        );
    
        Vec3 realCameraPos = camera.getPosition();
        Vec3 isometricAdjustedOriginalCameraPos =
            TransformationManager.getIsometricAdjustedCameraPos(camera);
        
        Vec3 physicalPlayerHeadPos = ClientTeleportationManager.getPlayerEyePos(RenderStates.tickDelta);
        
        Pair<Portal, Vec3> portalHit = PortalCommand.raytracePortals(
            client.level, physicalPlayerHeadPos, isometricAdjustedOriginalCameraPos, true
        ).orElse(null);
        
        if (portalHit == null) {
            return false;
        }
        
        Portal portal = portalHit.getFirst();
        Vec3 hitPos = portalHit.getSecond();
        
        if (!portal.canTeleportEntity(cameraEntity)) {
            return false;
        }
        
        Vec3 renderingCameraPos;
        
        if (isThirdPerson()) {
            double distance = getThirdPersonMaxDistance();
            
            Vec3 thirdPersonPos = realCameraPos.subtract(physicalPlayerHeadPos).normalize()
                .scale(distance).add(physicalPlayerHeadPos);
            
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
            client.options.getEffectiveRenderDistance(),
            ((IEGameRenderer) client.gameRenderer).getDoRenderHand(),
            false
        );
        
        IPCGlobal.renderer.invokeWorldRendering(worldRenderInfo);
        
        return true;
    }
    
    private static boolean isFrontView() {
        return client.options.getCameraType().isMirrored();
    }
    
    private static boolean isThirdPerson() {
        return !client.options.getCameraType().isFirstPerson();
    }
    
    /**
     * {@link Camera#getMaxZoom(double)}
     */
    private static Vec3 getThirdPersonCameraPos(Vec3 endPos, Portal portal, Vec3 startPos) {
        Vec3 rtStart = portal.transformPoint(startPos);
        Vec3 rtEnd = portal.transformPoint(endPos);
        BlockHitResult blockHitResult = portal.getDestinationWorld().clip(
            new ClipContext(
                rtStart,
                rtEnd,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                client.cameraEntity
            )
        );
        
        if (blockHitResult == null) {
            return rtStart.add(rtEnd.subtract(rtStart).normalize().scale(
                getThirdPersonMaxDistance()
            ));
        }
        
        return blockHitResult.getLocation();
    }
    
    private static double getThirdPersonMaxDistance() {
        return 4.0d * PehkuiInterface.invoker.computeThirdPersonScale(client.player, client.getFrameTime());
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
