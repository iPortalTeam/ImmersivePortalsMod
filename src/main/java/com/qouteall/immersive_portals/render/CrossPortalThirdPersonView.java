package com.qouteall.immersive_portals.render;

import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.commands.PortalCommand;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class CrossPortalThirdPersonView {
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    // if rendered, return true
    public static boolean renderCrossPortalThirdPersonView() {
        if (!isThirdPerson()) {
            return false;
        }
        
        Entity cameraEntity = client.cameraEntity;
        
        Camera camera = new Camera();
        float cameraY = ((IECamera) RenderStates.originalCamera).getCameraY();
        ((IECamera) camera).setCameraY(cameraY, cameraY);
        camera.update(
            client.world, cameraEntity,
            true,
            client.options.perspective == 2,
            RenderStates.tickDelta
        );
        Vec3d thirdPersonPos = camera.getPos();
        
        camera.update(
            client.world, cameraEntity,
            false, false, RenderStates.tickDelta
        );
        Vec3d playerHeadPos = camera.getPos();
        
        Pair<Portal, Vec3d> portalHit = PortalCommand.raytracePortals(
            client.world, playerHeadPos, thirdPersonPos, true
        ).orElse(null);
        
        if (portalHit == null) {
            return false;
        }
        
        Portal portal = portalHit.getFirst();
        
        if (!portal.isInteractable()) {
            return false;
        }
    
        Vec3d renderingCameraPos = portal.transformPoint(thirdPersonPos);
        ((IECamera) RenderStates.originalCamera).portal_setPos(renderingCameraPos);
        MyGameRenderer.renderWorldNew(
            new RenderInfo(
                CGlobal.clientWorldLoader.getWorld(portal.dimensionTo),
                renderingCameraPos,
                null,
                portal
            ),
            Runnable::run
        );
        
        return true;
    }
    
    private static boolean isThirdPerson() {
        return client.options.perspective > 0;
    }
    
}
