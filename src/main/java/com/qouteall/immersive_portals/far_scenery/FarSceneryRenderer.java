package com.qouteall.immersive_portals.far_scenery;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL13;

import static org.lwjgl.opengl.GL11.GL_QUADS;

@Deprecated
public class FarSceneryRenderer {
    public static boolean shouldUpdateFarScenery = true;
    
    private enum State {
        notReady,
        notReadyAndRendering1,
        presenting1AndRendering2,
        presenting2AndRendering1
    }
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static SecondaryFrameBuffer[] fbSet1;
    private static SecondaryFrameBuffer[] fbSet2;
    private static State state = State.notReady;
    private static MyTaskList.MyTask renderingTask;
    
    public static void init() {
        fbSet1 = new SecondaryFrameBuffer[6];
        fbSet1[0] = new SecondaryFrameBuffer();
        fbSet1[1] = new SecondaryFrameBuffer();
        fbSet1[2] = new SecondaryFrameBuffer();
        fbSet1[3] = new SecondaryFrameBuffer();
        fbSet1[4] = new SecondaryFrameBuffer();
        fbSet1[5] = new SecondaryFrameBuffer();
        
        fbSet2 = new SecondaryFrameBuffer[6];
        fbSet2[0] = new SecondaryFrameBuffer();
        fbSet2[1] = new SecondaryFrameBuffer();
        fbSet2[2] = new SecondaryFrameBuffer();
        fbSet2[3] = new SecondaryFrameBuffer();
        fbSet2[4] = new SecondaryFrameBuffer();
        fbSet2[5] = new SecondaryFrameBuffer();
    }
    
    public static void resetRenderingTask(SecondaryFrameBuffer[] frameBufferSet) {
        for (SecondaryFrameBuffer fb : frameBufferSet) {
            fb.prepare(1000, 1000);
        }
    
        Vec3d currCameraPos = mc.gameRenderer.getCamera().getPos();
        renderingTask = FaceRenderingTask.createFarSceneryRenderingTask(
            currCameraPos,
            mc.world.getDimension().getType(),
            100,
            mc.options.viewDistance,
            frameBufferSet
        );
        FSRenderingContext.cameraPos = currCameraPos;
    }
    
    public static void setPlayerRotation(
        Direction direction,
        ClientPlayerEntity entity
    ) {
        switch (direction) {
            case DOWN:
                entity.pitch = 90;
                entity.yaw = 0;
                break;
            case UP:
                entity.pitch = -90;
                entity.yaw = 0;
                break;
            case NORTH:
                entity.pitch = 0;
                entity.yaw = 180;
                break;
            case SOUTH:
                entity.pitch = 0;
                entity.yaw = 0;
                break;
            case WEST:
                entity.pitch = 0;
                entity.yaw = 90;
                break;
            case EAST:
                entity.pitch = 0;
                entity.yaw = -90;
                break;
        }
    }
    
    public static double[] getCullingEquation() {
        return FSRenderingContext.cullingEquation;
    }
    
    public static void updateCullingEquation(double distance, Direction direction) {
        Vec3d planeNormal =  Vec3d.of(direction.getVector());
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        
        Vec3d portalPos = planeNormal.multiply(distance);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
    
        FSRenderingContext.cullingEquation = new double[]{
            planeNormal.x,
            planeNormal.y,
            planeNormal.z,
            c
        };
    }
    
    private static void renderFarSceneryBox(
        MatrixStack matrixStack,
        double distance,
        Vec3d offsetToCamera,
        SecondaryFrameBuffer[] frameBuffers
    ) {
        matrixStack.push();
        matrixStack.translate(
            -offsetToCamera.x, -offsetToCamera.y, -offsetToCamera.z
        );
        matrixStack.scale((float) distance, (float) distance, (float) distance);
    
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
//        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableFog();
        RenderSystem.shadeModel(7425);
    
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
    
        bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(-1.0D, -1.0D, 1.0D).texture(1.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, 1.0D, 1.0D).texture(1.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, 1.0D, 1.0D).texture(0.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, -1.0D, 1.0D).texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        myBindTexture(frameBuffers[Direction.SOUTH.ordinal()]);
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        
        bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(1.0D, -1.0D, 1.0D).texture(1.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, 1.0D, 1.0D).texture(1.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, 1.0D, -1.0D).texture(0.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, -1.0D, -1.0D).texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        myBindTexture(frameBuffers[Direction.EAST.ordinal()]);
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        
        bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(1.0D, -1.0D, -1.0D).texture(1.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, 1.0D, -1.0D).texture(1.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, 1.0D, -1.0D).texture(0.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, -1.0D, -1.0D).texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        myBindTexture(frameBuffers[Direction.NORTH.ordinal()]);
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        
        bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(-1.0D, -1.0D, -1.0D).texture(1.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, 1.0D, -1.0D).texture(1.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, 1.0D, 1.0D).texture(0.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, -1.0D, 1.0D).texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        myBindTexture(frameBuffers[Direction.WEST.ordinal()]);
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        
        bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(-1.0D, -1.0D, -1.0D).texture(1.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, -1.0D, 1.0D).texture(1.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, -1.0D, 1.0D).texture(0.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, -1.0D, -1.0D).texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        myBindTexture(frameBuffers[Direction.DOWN.ordinal()]);
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        
        bufferBuilder.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(-1.0D, 1.0D, 1.0D).texture(1.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(-1.0D, 1.0D, -1.0D).texture(1.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, 1.0D, -1.0D).texture(0.0F, 1.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(1.0D, 1.0D, 1.0D).texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        myBindTexture(frameBuffers[Direction.UP.ordinal()]);
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        
        matrixStack.pop();
    }
    
    private static void myBindTexture(SecondaryFrameBuffer fb) {
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        
        GlStateManager.bindTexture(fb.fb.colorAttachment);
        GlStateManager.texParameter(3553, 10241, 9729);
        GlStateManager.texParameter(3553, 10240, 9729);
        GlStateManager.texParameter(3553, 10242, 10496);
        GlStateManager.texParameter(3553, 10243, 10496);
    }
    
    public static void onBeforeTranslucentRendering(
        MatrixStack matrixStack
    ) {
        if (!FSRenderingContext.isFarSceneryEnabled) {
            return;
        }
        if (FSRenderingContext.isRenderingScenery) {
            return;
        }
        if (CGlobal.renderer.isRendering()) {
            return;
        }
    
        if (shouldUpdateFarScenery) {
            updateFarScenery();
        }
    
        Vec3d currCameraPos = mc.gameRenderer.getCamera().getPos();
        if (state == State.presenting1AndRendering2) {
            renderFarSceneryBox(
                matrixStack, FSRenderingContext.nearPlaneDistance,
                currCameraPos.subtract(FSRenderingContext.cameraPos), fbSet1
            );
        }
        else if (state == State.presenting2AndRendering1) {
            renderFarSceneryBox(
                matrixStack, FSRenderingContext.nearPlaneDistance,
                currCameraPos.subtract(FSRenderingContext.cameraPos), fbSet2
            );
        }
    }
    
    private static void updateFarScenery() {
        if (state == State.notReady) {
            resetRenderingTask(fbSet1);
            state = State.notReadyAndRendering1;
        }
        else if (state == State.notReadyAndRendering1) {
            boolean succeeded = renderingTask.runAndGetIsSucceeded();
            if (succeeded) {
                FaceRenderingTask.scheduler.onRenderSucceeded();
                state = State.presenting1AndRendering2;
                resetRenderingTask(fbSet2);
            }
        }
        else if (state == State.presenting1AndRendering2) {
            boolean succeeded = renderingTask.runAndGetIsSucceeded();
            if (succeeded) {
                FaceRenderingTask.scheduler.onRenderSucceeded();
                state = State.presenting2AndRendering1;
                resetRenderingTask(fbSet1);
            }
        }
        else if (state == State.presenting2AndRendering1) {
            boolean succeeded = renderingTask.runAndGetIsSucceeded();
            if (succeeded) {
                FaceRenderingTask.scheduler.onRenderSucceeded();
                state = State.presenting1AndRendering2;
                resetRenderingTask(fbSet2);
            }
        }
        else {
            assert false;
        }
    }
}
