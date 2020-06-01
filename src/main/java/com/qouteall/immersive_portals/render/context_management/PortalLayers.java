package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.TransformationManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PortalLayers {
    private static final Stack<Portal> portalLayers = new Stack<>();
    private static boolean isRenderingCache = false;
    
    public static void pushPortalLayer(Portal portal) {
        portalLayers.push(portal);
        isRenderingCache = getPortalLayer() != 0;
    }
    
    public static void popPortalLayer() {
        portalLayers.pop();
        isRenderingCache = getPortalLayer() != 0;
    }
    
    //0 for rendering outer world
    //1 for rendering world inside portal
    //2 for rendering world inside the portal inside portal
    public static int getPortalLayer() {
        return portalLayers.size();
    }
    
    public static boolean isRendering() {
        return isRenderingCache;
    }
    
    public static int getMaxPortalLayer() {
        if (RenderStates.isLaggy) {
            return 1;
        }
        return Global.maxPortalLayer;
    }
    
    public static Portal getRenderingPortal() {
        return portalLayers.peek();
    }
    
    public static void onBeginPortalWorldRendering() {
        List<WeakReference<Portal>> currRenderInfo = portalLayers.stream().map(
            (Function<Portal, WeakReference<Portal>>) WeakReference::new
        ).collect(Collectors.toList());
        RenderStates.portalRenderInfos.add(currRenderInfo);
        
        CHelper.checkGlError();
    }
    
    public static void onEndPortalWorldRendering() {
        RenderStates.renderedDimensions.add(
            portalLayers.peek().dimensionTo
        );
    }
    
    public static boolean isRenderingOddNumberOfMirrors() {
        Stack<Portal> portalLayers = PortalLayers.portalLayers;
        int number = 0;
        for (Portal portal : portalLayers) {
            if (portal instanceof Mirror) {
                number++;
            }
        }
        return number % 2 == 1;
    }
    
    public static void adjustCameraPos(Camera camera) {
        Vec3d pos = RenderStates.originalCamera.getPos();
        for (Portal portal : portalLayers) {
            pos = portal.transformPoint(pos);
        }
        ((IECamera) camera).mySetPos(pos);
    }
    
    public static void applyAdditionalTransformations(MatrixStack matrixStack) {
        portalLayers.forEach(portal -> {
            if (portal instanceof Mirror) {
                Matrix4f matrix = TransformationManager.getMirrorTransformation(portal.getNormal());
                matrixStack.peek().getModel().multiply(matrix);
                matrixStack.peek().getNormal().multiply(new Matrix3f(matrix));
            }
            else if (portal.rotation != null) {
                Quaternion rot = portal.rotation.copy();
                rot.conjugate();
                matrixStack.multiply(rot);
            }
        });
    }
}
