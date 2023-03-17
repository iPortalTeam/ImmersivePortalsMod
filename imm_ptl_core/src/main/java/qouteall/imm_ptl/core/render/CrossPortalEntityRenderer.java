package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CrossPortalEntityRenderer {
    private static final Minecraft client = Minecraft.getInstance();
    
    //there is no weak hash set
    private static final WeakHashMap<Entity, Object> collidedEntities = new WeakHashMap<>();
    
    public static boolean isRenderingEntityNormally = false;
    
    public static boolean isRenderingEntityProjection = false;
    
    public static void init() {
        IPGlobal.postClientTickSignal.connect(CrossPortalEntityRenderer::onClientTick);
        
        IPGlobal.clientCleanupSignal.connect(CrossPortalEntityRenderer::cleanUp);
        
        ClientWorldLoader.clientDimensionDynamicRemoveSignal.connect(dim -> cleanUp());
    }
    
    private static void cleanUp() {
        collidedEntities.clear();
    }
    
    private static void onClientTick() {
        collidedEntities.entrySet().removeIf(entry ->
            entry.getKey().isRemoved() ||
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
    
    public static void onBeginRenderingEntities(PoseStack matrixStack) {
        isRenderingEntityNormally = true;
        
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getRenderingPortal(), false, matrixStack
            );
        }
    }
    
    private static boolean isCrossPortalRenderingEnabled() {
        if (IrisInterface.invoker.isIrisPresent()) {
            return false;
        }
        return IPGlobal.correctCrossPortalEntityRendering;
    }
    
    // do not use runWithTransformation here (because matrixStack is changed?)
    public static void onEndRenderingEntities(PoseStack matrixStack) {
        isRenderingEntityNormally = false;
        
        FrontClipping.disableClipping();
        
        if (!isCrossPortalRenderingEnabled()) {
            return;
        }
        
        renderEntityProjections(matrixStack);
    }
    
    public static void beforeRenderingEntity(Entity entity, PoseStack matrixStack) {
        if (!isCrossPortalRenderingEnabled()) {
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
                client.renderBuffers().bufferSource().endBatch();
                
                FrontClipping.setupOuterClipping(matrixStack, collidingPortal);
            }
        }
    }
    
    public static void afterRenderingEntity(Entity entity) {
        if (!isCrossPortalRenderingEnabled()) {
            return;
        }
        if (!PortalRendering.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                //draw it with culling in a separate draw call
                client.renderBuffers().bufferSource().endBatch();
                FrontClipping.disableClipping();
            }
        }
    }
    
    //if an entity is in overworld but halfway through a nether portal
    //then it has a projection in nether
    private static void renderEntityProjections(PoseStack matrixStack) {
        if (!isCrossPortalRenderingEnabled()) {
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
            ResourceKey<Level> projectionDimension = collidingPortal.dimensionTo;
            if (client.level.dimension() == projectionDimension) {
                renderProjectedEntity(entity, collidingPortal, matrixStack);
            }
        });
    }
    
    public static boolean hasIntersection(
        Vec3 outerPlanePos, Vec3 outerPlaneNormal,
        Vec3 entityPos, Vec3 collidingPortalNormal
    ) {
        return entityPos.subtract(outerPlanePos).dot(outerPlaneNormal) > 0.01 &&
            outerPlanePos.subtract(entityPos).dot(collidingPortalNormal) > 0.01;
    }
    
    private static void renderProjectedEntity(
        Entity entity,
        Portal collidingPortal,
        PoseStack matrixStack
    ) {
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            //correctly rendering it needs two culling planes
            //use some rough check to work around
            
            if (renderingPortal instanceof Portal) {
                if (!Portal.isFlippedPortal(((Portal) renderingPortal), collidingPortal)
                    && !Portal.isReversePortal(((Portal) renderingPortal), collidingPortal)
                ) {
                    Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
                    
                    boolean isHidden = cameraPos.subtract(collidingPortal.getDestPos())
                        .dot(collidingPortal.getContentDirection()) < 0;
                    if (renderingPortal == collidingPortal || !isHidden) {
                        renderEntity(entity, collidingPortal, matrixStack);
                    }
                }
            }
        }
        else {
            FrontClipping.disableClipping();
            // don't draw the existing triangles with culling enabled
            client.renderBuffers().bufferSource().endBatch();
            
            FrontClipping.setupInnerClipping(collidingPortal, false, matrixStack);
            renderEntity(entity, collidingPortal, matrixStack);
            FrontClipping.disableClipping();
        }
    }
    
    private static void renderEntity(
        Entity entity,
        Portal transformingPortal,
        PoseStack matrixStack
    ) {
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        
        ClientLevel newWorld = ClientWorldLoader.getWorld(transformingPortal.dimensionTo);
        
        Vec3 oldEyePos = McHelper.getEyePos(entity);
        Vec3 oldLastTickEyePos = McHelper.getLastTickEyePos(entity);
        Level oldWorld = entity.level;
        
        Vec3 newEyePos = transformingPortal.transformPoint(oldEyePos);
        
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            
            Vec3 transformedEntityPos = newEyePos.subtract(McHelper.getEyeOffset(entity));
            AABB transformedBoundingBox = McHelper.getBoundingBoxWithMovedPosition(entity, transformedEntityPos);
            
            boolean intersects = PortalManipulation.isOtherSideBoxInside(transformedBoundingBox, renderingPortal);
            
            if (!intersects) {
                return;
            }
        }
        
        if (entity instanceof LocalPlayer) {
            if (!IPGlobal.renderYourselfInPortal) {
                return;
            }
            
            if (!transformingPortal.getDoRenderPlayer()) {
                return;
            }
            
            if (client.options.getCameraType().isFirstPerson()) {
                //avoid rendering player too near and block view
                double dis = newEyePos.distanceTo(cameraPos);
                double valve = 0.5 + McHelper.lastTickPosOf(entity).distanceTo(entity.position());
                if (transformingPortal.scaling > 1) {
                    valve *= transformingPortal.scaling;
                }
                if (dis < valve) {
                    return;
                }
                
                AABB transformedBoundingBox =
                    Helper.transformBox(RenderStates.originalPlayerBoundingBox, transformingPortal::transformPoint);
                if (transformedBoundingBox.contains(CHelper.getCurrentCameraPos())) {
                    return;
                }
            }
        }
        
        McHelper.setEyePos(
            entity,
            newEyePos,
            transformingPortal.transformPoint(oldLastTickEyePos)
        );
        
        entity.level = newWorld;
        
        isRenderingEntityProjection = true;
        matrixStack.pushPose();
        setupEntityProjectionRenderingTransformation(
            transformingPortal, entity, matrixStack
        );
        
        MultiBufferSource.BufferSource consumers = client.renderBuffers().bufferSource();
        ((IEWorldRenderer) client.levelRenderer).ip_myRenderEntity(
            entity,
            cameraPos.x, cameraPos.y, cameraPos.z,
            RenderStates.tickDelta, matrixStack,
            consumers
        );
        //immediately invoke draw call
        consumers.endBatch();
        
        matrixStack.popPose();
        isRenderingEntityProjection = false;
        
        McHelper.setEyePos(
            entity, oldEyePos, oldLastTickEyePos
        );
        entity.level = oldWorld;
    }
    
    private static void setupEntityProjectionRenderingTransformation(
        Portal portal, Entity entity, PoseStack matrixStack
    ) {
        if (portal.scaling == 1.0 && portal.getRotation() == null) {
            return;
        }
        
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        
        Vec3 anchor = entity.getEyePosition(RenderStates.tickDelta).subtract(cameraPos);
        
        matrixStack.translate(anchor.x, anchor.y, anchor.z);
        
        float scaling = (float) portal.scaling;
        matrixStack.scale(scaling, scaling, scaling);
        
        if (portal.getRotation() != null) {
            matrixStack.mulPose(portal.getRotation().toMcQuaternion());
        }
        
        matrixStack.translate(-anchor.x, -anchor.y, -anchor.z);
    }
    
    public static boolean shouldRenderPlayerDefault() {
        if (!IPGlobal.renderYourselfInPortal) {
            return false;
        }
        if (!WorldRenderInfo.isRendering()) {
            return false;
        }
        LocalPlayer player = client.player;
        assert player != null;
        
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            if (renderingPortal instanceof Mirror) {
                // if the camera pos is too close to the mirror,
                // it will show the inside of the player head.
                // avoid rendering player in this case.
                float width = player.getBbWidth();
                if (renderingPortal.getDistanceToNearestPointInPortal(player.getEyePosition()) < width * 0.8) {
                    return false;
                }
            }
        }
        
        if (client.level == player.level) {
            return true;
        }
        
        return false;
    }
    
    public static boolean shouldRenderEntityNow(Entity entity) {
        Validate.notNull(entity);
        if (IrisInterface.invoker.isRenderingShadowMap()) {
            return true;
        }
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
            
            if (entity instanceof Player && !renderingPortal.getDoRenderPlayer()) {
                return false;
            }
            
            // client colliding portal update is not immediate
            if (collidingPortal != null && !(entity instanceof LocalPlayer)) {
                if (renderingPortal instanceof Portal) {
                    if (!Portal.isReversePortal(collidingPortal, ((Portal) renderingPortal))) {
                        Vec3 cameraPos = PortalRenderer.client.gameRenderer.getMainCamera().getPosition();
                        
                        boolean isHidden = cameraPos.subtract(collidingPortal.getOriginPos())
                            .dot(collidingPortal.getNormal()) < 0;
                        if (isHidden) {
                            return false;
                        }
                    }
                }
            }
            
            return renderingPortal.isInside(
                getRenderingCameraPos(entity), -0.01
            );
        }
        return true;
    }
    
    public static boolean shouldRenderPlayerNormally(Entity entity) {
        if (!client.options.getCameraType().isFirstPerson()) {
            return true;
        }
        
        if (RenderStates.originalPlayerBoundingBox.contains(CHelper.getCurrentCameraPos())) {
            return false;
        }
        
        double distanceToCamera =
            getRenderingCameraPos(entity)
                .distanceTo(client.gameRenderer.getMainCamera().getPosition());
        //avoid rendering player too near and block view except mirror
        return distanceToCamera > 1 || PortalRendering.isRenderingOddNumberOfMirrors();
    }
    
    public static Vec3 getRenderingCameraPos(Entity entity) {
        if (entity instanceof LocalPlayer) {
            return RenderStates.originalPlayerPos.add(
                McHelper.getEyeOffset(entity)
            );
        }
        return entity.getEyePosition(RenderStates.tickDelta);
    }
}
