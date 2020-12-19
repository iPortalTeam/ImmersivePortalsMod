package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.ShaderClippingManager;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CrossPortalEntityRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    //there is no weak hash set
    private static final WeakHashMap<Entity, Object> collidedEntities = new WeakHashMap<>();
    
    public static boolean isRendering = false;
    
    public static void init() {
        ModMain.postClientTickSignal.connect(CrossPortalEntityRenderer::onClientTick);
        
        ModMain.clientCleanupSignal.connect(CrossPortalEntityRenderer::cleanUp);
    }
    
    private static void cleanUp() {
        collidedEntities.clear();
    }
    
    private static void onClientTick() {
        collidedEntities.entrySet().removeIf(entry ->
            entry.getKey().removed ||
                ((IEEntity) entry.getKey()).getCollidingPortal() == null
        );
    }
    
    public static void onEntityTickClient(Entity entity) {
        if (entity instanceof Portal) {
            return;
        }
        
        Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            collidedEntities.put(entity, null);
        }
    }
    
    public static void onBeginRenderingEntities(MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                matrixStack, PortalRendering.getRenderingPortal(), false
            );
        }
    }
    
    // do not use runWithTransformation here (because matrixStack is changed?)
    public static void onEndRenderingEntities(MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        
        renderEntityProjections(matrixStack);
    }
    
    public static void beforeRenderingEntity(Entity entity, MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        if (!PortalRendering.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
                if (collidingPortal == null) {
                    //Helper.err("Colliding Portal Record Invalid " + entity);
                    return;
                }
                
                //draw already built triangles
                client.getBufferBuilders().getEntityVertexConsumers().draw();
    
                FrontClipping.setupOuterClipping(matrixStack, collidingPortal);
                if (OFInterface.isShaders.getAsBoolean()) {
                    ShaderClippingManager.update();
                }
            }
        }
    }
    
    public static void afterRenderingEntity(Entity entity) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        if (!PortalRendering.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                //draw it with culling in a separate draw call
                client.getBufferBuilders().getEntityVertexConsumers().draw();
                FrontClipping.disableClipping();
            }
        }
    }
    
    //if an entity is in overworld but halfway through a nether portal
    //then it has a projection in nether
    private static void renderEntityProjections(MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        collidedEntities.keySet().forEach(entity -> {
            Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
            if (collidingPortal == null) {
                //Helper.err("Colliding Portal Record Invalid " + entity);
                return;
            }
            if (collidingPortal instanceof Mirror) {
                //no need to render entity projection for mirrors
                return;
            }
            RegistryKey<World> projectionDimension = collidingPortal.dimensionTo;
            if (client.world.getRegistryKey() == projectionDimension) {
                renderProjectedEntity(entity, collidingPortal, matrixStack);
            }
        });
    }
    
    public static boolean hasIntersection(
        Vec3d outerPlanePos, Vec3d outerPlaneNormal,
        Vec3d entityPos, Vec3d collidingPortalNormal
    ) {
        return entityPos.subtract(outerPlanePos).dotProduct(outerPlaneNormal) > 0.01 &&
            outerPlanePos.subtract(entityPos).dotProduct(collidingPortalNormal) > 0.01;
    }
    
    private static void renderProjectedEntity(
        Entity entity,
        Portal collidingPortal,
        MatrixStack matrixStack
    ) {
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            //correctly rendering it needs two culling planes
            //use some rough check to work around
            
            if(renderingPortal instanceof Portal) {
                if (!Portal.isFlippedPortal(((Portal) renderingPortal), collidingPortal)) {
                    Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
                    
                    boolean isHidden = cameraPos.subtract(collidingPortal.getDestPos())
                        .dotProduct(collidingPortal.getContentDirection()) < 0;
                    if (renderingPortal == collidingPortal || !isHidden) {
                        renderEntityRegardingPlayer(entity, collidingPortal, matrixStack);
                    }
                }
            }
        }
        else {
            FrontClipping.disableClipping();
            // don't draw the existing triangles with culling enabled
            client.getBufferBuilders().getEntityVertexConsumers().draw();
    
            FrontClipping.setupInnerClipping(matrixStack, collidingPortal, false);
            renderEntityRegardingPlayer(entity, collidingPortal, matrixStack);
            FrontClipping.disableClipping();
        }
    }
    
    private static void renderEntityRegardingPlayer(
        Entity entity,
        Portal transformingPortal,
        MatrixStack matrixStack
    ) {
        if (entity instanceof ClientPlayerEntity) {
            MyGameRenderer.renderPlayerItself(() -> {
                renderEntity(entity, transformingPortal, matrixStack);
            });
        }
        else {
            renderEntity(entity, transformingPortal, matrixStack);
        }
    }
    
    private static void renderEntity(
        Entity entity,
        Portal transformingPortal,
        MatrixStack matrixStack
    ) {
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
    
        ClientWorld newWorld = ClientWorldLoader.getWorld(transformingPortal.dimensionTo);
        
        Vec3d oldEyePos = McHelper.getEyePos(entity);
        Vec3d oldLastTickEyePos = McHelper.getLastTickEyePos(entity);
        World oldWorld = entity.world;
        
        Vec3d newEyePos = transformingPortal.transformPoint(oldEyePos);

//        if (PortalRendering.isRendering()) {
//            Portal renderingPortal = PortalRendering.getRenderingPortal();
//            if (!renderingPortal.isInside(newEyePos, -3)) {
//                return;
//            }
//        }
        
        if (entity instanceof ClientPlayerEntity) {
            if (!Global.renderYourselfInPortal) {
                return;
            }
            
            if (client.options.getPerspective().isFirstPerson()) {
                //avoid rendering player too near and block view
                double dis = newEyePos.distanceTo(cameraPos);
                double valve = 0.5 + McHelper.lastTickPosOf(entity).distanceTo(entity.getPos());
                if (transformingPortal.scaling > 1) {
                    valve *= transformingPortal.scaling;
                }
                if (dis < valve) {
                    return;
                }
            }
        }
        
        McHelper.setEyePos(
            entity,
            newEyePos,
            transformingPortal.transformPoint(oldLastTickEyePos)
        );
        
        entity.world = newWorld;
        
        isRendering = true;
        matrixStack.push();
        setupEntityProjectionRenderingTransformation(
            transformingPortal, entity, matrixStack
        );
        
        OFInterface.updateEntityTypeForShader.accept(entity);
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        ((IEWorldRenderer) client.worldRenderer).myRenderEntity(
            entity,
            cameraPos.x, cameraPos.y, cameraPos.z,
            RenderStates.tickDelta, matrixStack,
            consumers
        );
        //immediately invoke draw call
        consumers.draw();
        
        matrixStack.pop();
        isRendering = false;
        
        McHelper.setEyePos(
            entity, oldEyePos, oldLastTickEyePos
        );
        entity.world = oldWorld;
    }
    
    private static void setupEntityProjectionRenderingTransformation(
        Portal portal, Entity entity, MatrixStack matrixStack
    ) {
        if (portal.scaling == 1.0 && portal.rotation == null) {
            return;
        }
        
        Vec3d cameraPos = CHelper.getCurrentCameraPos();
        
        Vec3d anchor = entity.getCameraPosVec(RenderStates.tickDelta).subtract(cameraPos);
        
        matrixStack.translate(anchor.x, anchor.y, anchor.z);
        
        float scaling = (float) portal.scaling;
        matrixStack.scale(scaling, scaling, scaling);
        
        if (portal.rotation != null) {
            matrixStack.multiply(portal.rotation);
        }
        
        matrixStack.translate(-anchor.x, -anchor.y, -anchor.z);
    }
    
    public static boolean shouldRenderPlayerItself() {
        if (!Global.renderYourselfInPortal) {
            return false;
        }
        if (!PortalRendering.isRendering()) {
            return false;
        }
        if (client.cameraEntity.world.getRegistryKey() == RenderStates.originalPlayerDimension) {
            return true;
        }
        return false;
    }
    
    public static boolean shouldRenderEntityNow(Entity entity) {
        if (OFInterface.isShadowPass.getAsBoolean()) {
            return true;
        }
        if (PortalRendering.isRendering()) {
            if (entity instanceof ClientPlayerEntity) {
                return shouldRenderPlayerItself();
            }
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
            if (collidingPortal != null) {
                if(renderingPortal instanceof Portal) {
                    if (!Portal.isReversePortal(collidingPortal, ((Portal) renderingPortal))) {
                        Vec3d cameraPos = PortalRenderer.client.gameRenderer.getCamera().getPos();
        
                        boolean isHidden = cameraPos.subtract(collidingPortal.getOriginPos())
                            .dotProduct(collidingPortal.getNormal()) < 0;
                        if (isHidden) {
                            return false;
                        }
                    }
                }
            }
            
            return renderingPortal.isInside(
                entity.getCameraPosVec(RenderStates.tickDelta), -0.01
            );
        }
        return true;
    }
    
    public static boolean shouldRenderPlayerNormally(Entity entity) {
        if (!client.options.getPerspective().isFirstPerson()) {
            return true;
        }
        
        double distanceToCamera =
            entity.getCameraPosVec(RenderStates.tickDelta)
                .distanceTo(client.gameRenderer.getCamera().getPos());
        //avoid rendering player too near and block view except mirror
        return distanceToCamera > 1 || PortalRendering.isRenderingOddNumberOfMirrors();
    }
}
