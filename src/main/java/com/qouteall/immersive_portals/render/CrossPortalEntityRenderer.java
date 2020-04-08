package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.ShaderCullingManager;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CrossPortalEntityRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    //there is no weak hash set
    private static final WeakHashMap<Entity, Object> collidedEntities = new WeakHashMap<>();
    
    public static boolean isRendering = false;
    
    public static void init() {
        ModMain.postClientTickSignal.connect(CrossPortalEntityRenderer::onClientTick);
    }
    
    public static void cleanUp() {
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
    
    public static void onBeginRenderingEnties(MatrixStack matrixStack) {
        if (CGlobal.renderer.isRendering()) {
            PixelCuller.updateCullingPlaneInner(matrixStack, CGlobal.renderer.getRenderingPortal());
            PixelCuller.startCulling();
        }
    }
    
    public static void onEndRenderingEntities(MatrixStack matrixStack) {
        PixelCuller.endCulling();
        
        renderEntityProjections(matrixStack);
    }
    
    public static void beforeRenderingEntity(Entity entity, MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        if (!CGlobal.renderer.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
                if (collidingPortal == null) {
                    //Helper.err("Colliding Portal Record Invalid " + entity);
                    return;
                }
                
                //draw already built triangles
                client.getBufferBuilders().getEntityVertexConsumers().draw();
                
                PixelCuller.updateCullingPlaneOuter(
                    matrixStack,
                    collidingPortal
                );
                PixelCuller.startCulling();
                if (OFInterface.isShaders.getAsBoolean()) {
                    ShaderCullingManager.update();
                }
            }
        }
    }
    
    public static void afterRenderingEntity(Entity entity) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        if (!CGlobal.renderer.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                //draw it with culling in a separate draw call
                client.getBufferBuilders().getEntityVertexConsumers().draw();
                PixelCuller.endCulling();
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
            DimensionType projectionDimension = collidingPortal.dimensionTo;
            if (client.world.dimension.getType() == projectionDimension) {
                renderProjectedEntity(entity, collidingPortal, matrixStack);
            }
        });
    }
    
    private static void renderProjectedEntity(
        Entity entity,
        Portal collidingPortal,
        MatrixStack matrixStack
    ) {
        if (CGlobal.renderer.isRendering()) {
            //???
            if (collidingPortal == CGlobal.renderer.getRenderingPortal()
                || (entity instanceof ClientPlayerEntity)
            ) {
                renderEntityRegardingPlayer(entity, collidingPortal, matrixStack);
            }
        }
        else {
            PixelCuller.updateCullingPlaneInner(matrixStack, collidingPortal);
            PixelCuller.startCulling();
            renderEntityRegardingPlayer(entity, collidingPortal, matrixStack);
            PixelCuller.endCulling();
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
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(
            transformingPortal.dimensionTo
        );
        
        Vec3d oldEyePos = McHelper.getEyePos(entity);
        Vec3d oldLastTickEyePos = McHelper.getLastTickEyePos(entity);
        World oldWorld = entity.world;
        
        Vec3d newEyePos = transformingPortal.transformPoint(oldEyePos);
        
        if (entity instanceof ClientPlayerEntity) {
            //avoid rendering player too near and block view
            if (newEyePos.squaredDistanceTo(cameraPos) < 0.5 + entity.getVelocity().lengthSquared()) {
                return;
            }
        }
        
        McHelper.setEyePos(
            entity,
            newEyePos,
            transformingPortal.transformPoint(oldLastTickEyePos)
        );
        
        entity.world = newWorld;
        
        isRendering = true;
        OFInterface.updateEntityTypeForShader.accept(entity);
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        ((IEWorldRenderer) client.worldRenderer).myRenderEntity(
            entity,
            cameraPos.x, cameraPos.y, cameraPos.z,
            MyRenderHelper.tickDelta, matrixStack,
            consumers
        );
        //immediately invoke draw call
        consumers.draw();
        isRendering = false;
        
        McHelper.setEyePos(
            entity, oldEyePos, oldLastTickEyePos
        );
        entity.world = oldWorld;
    }
}
