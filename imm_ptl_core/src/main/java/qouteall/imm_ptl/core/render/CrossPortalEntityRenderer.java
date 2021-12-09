package qouteall.imm_ptl.core.render;

import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CrossPortalEntityRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    //there is no weak hash set
    private static final WeakHashMap<Entity, Object> collidedEntities = new WeakHashMap<>();
    
    public static boolean isRenderingEntityNormally = false;
    
    public static boolean isRenderingEntityProjection = false;
    
    public static void init() {
        IPGlobal.postClientTickSignal.connect(CrossPortalEntityRenderer::onClientTick);
        
        IPGlobal.clientCleanupSignal.connect(CrossPortalEntityRenderer::cleanUp);
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
    
    public static void onBeginRenderingEntities(MatrixStack matrixStack) {
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
    public static void onEndRenderingEntities(MatrixStack matrixStack) {
        isRenderingEntityNormally = false;
        
        FrontClipping.disableClipping();
        
        if (!isCrossPortalRenderingEnabled()) {
            return;
        }
        
        renderEntityProjections(matrixStack);
    }
    
    public static void beforeRenderingEntity(Entity entity, MatrixStack matrixStack) {
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
                client.getBufferBuilders().getEntityVertexConsumers().draw();
                
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
                client.getBufferBuilders().getEntityVertexConsumers().draw();
                FrontClipping.disableClipping();
            }
        }
    }
    
    //if an entity is in overworld but halfway through a nether portal
    //then it has a projection in nether
    private static void renderEntityProjections(MatrixStack matrixStack) {
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
            
            if (renderingPortal instanceof Portal) {
                if (!Portal.isFlippedPortal(((Portal) renderingPortal), collidingPortal)
                    && !Portal.isReversePortal(((Portal) renderingPortal), collidingPortal)
                ) {
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
            
            FrontClipping.setupInnerClipping(collidingPortal, false, matrixStack);
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
        
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            
            Vec3d transformedEntityPos = newEyePos.subtract(0, entity.getStandingEyeHeight(), 0);
            Box transformedBoundingBox = McHelper.getBoundingBoxWithMovedPosition(entity, transformedEntityPos);
            
            boolean intersects = PortalManipulation.isOtherSideBoxInside(transformedBoundingBox, renderingPortal);
            
            if (!intersects) {
                return;
            }
        }
        
        if (entity instanceof ClientPlayerEntity) {
            if (!IPGlobal.renderYourselfInPortal) {
                return;
            }
            
            if (!transformingPortal.getDoRenderPlayer()) {
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
                
                Box transformedBoundingBox =
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
        
        entity.world = newWorld;
        
        isRenderingEntityProjection = true;
        matrixStack.push();
        setupEntityProjectionRenderingTransformation(
            transformingPortal, entity, matrixStack
        );
        
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        ((IEWorldRenderer) client.worldRenderer).ip_myRenderEntity(
            entity,
            cameraPos.x, cameraPos.y, cameraPos.z,
            RenderStates.tickDelta, matrixStack,
            consumers
        );
        //immediately invoke draw call
        consumers.draw();
        
        matrixStack.pop();
        isRenderingEntityProjection = false;
        
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
    
    public static boolean shouldRenderPlayerDefault() {
        if (!IPGlobal.renderYourselfInPortal) {
            return false;
        }
        if (!WorldRenderInfo.isRendering()) {
            return false;
        }
        if (client.cameraEntity.world.getRegistryKey() == RenderStates.originalPlayerDimension) {
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
            
            if (entity instanceof PlayerEntity && !renderingPortal.getDoRenderPlayer()) {
                return false;
            }
            
            // client colliding portal update is not immediate
            if (collidingPortal != null && !(entity instanceof ClientPlayerEntity)) {
                if (renderingPortal instanceof Portal) {
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
                getRenderingCameraPos(entity), -0.01
            );
        }
        return true;
    }
    
    public static boolean shouldRenderPlayerNormally(Entity entity) {
        if (!client.options.getPerspective().isFirstPerson()) {
            return true;
        }
        
        if (RenderStates.originalPlayerBoundingBox.contains(CHelper.getCurrentCameraPos())) {
            return false;
        }
        
        double distanceToCamera =
            getRenderingCameraPos(entity)
                .distanceTo(client.gameRenderer.getCamera().getPos());
        //avoid rendering player too near and block view except mirror
        return distanceToCamera > 1 || PortalRendering.isRenderingOddNumberOfMirrors();
    }
    
    public static Vec3d getRenderingCameraPos(Entity entity) {
        if (entity instanceof ClientPlayerEntity) {
            return RenderStates.originalPlayerPos.add(0, entity.getStandingEyeHeight(), 0);
        }
        return entity.getCameraPosVec(RenderStates.tickDelta);
    }
}
