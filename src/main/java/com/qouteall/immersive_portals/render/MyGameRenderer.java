package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.far_scenery.FSRenderingContext;
import com.qouteall.immersive_portals.far_scenery.FaceRenderingTask;
import com.qouteall.immersive_portals.far_scenery.FarSceneryRenderer;
import com.qouteall.immersive_portals.portal.Portal;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.opengl.GL11;

import java.util.function.Predicate;

public class MyGameRenderer {
    private MinecraftClient mc = MinecraftClient.getInstance();
    private double[] clipPlaneEquation;
    
    public MyGameRenderer() {
    
    }
    
    public static void doPruneVisibleChunks(ObjectList<?> visibleChunks) {
        if (CGlobal.renderer.isRendering()) {
            if (CGlobal.renderFewerInFastGraphic) {
                if (!MinecraftClient.getInstance().options.fancyGraphics) {
                    CGlobal.myGameRenderer.pruneVisibleChunksInFastGraphics(visibleChunks);
                }
            }
        }
        else if (FSRenderingContext.isFarSceneryEnabled) {
            CGlobal.myGameRenderer.pruneVisibleChunksForNearScenery(visibleChunks);
        }
    }
    
    public void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld,
        Vec3d oldCameraPos,
        ClientWorld oldWorld
    ) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ((IEWorldRenderer) newWorldRenderer).getBuiltChunkStorage().updateCameraPosition(
                mc.cameraEntity.getX(),
                mc.cameraEntity.getZ()
            );
        }
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) mc.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(newWorld.dimension.getType());
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        Camera newCamera = new Camera();
        
        //store old state
        WorldRenderer oldWorldRenderer = mc.worldRenderer;
        LightmapTextureManager oldLightmap = ieGameRenderer.getLightmapTextureManager();
        GameMode oldGameMode = playerListEntry.getGameMode();
        boolean oldNoClip = mc.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(newWorldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
        HitResult oldCrosshairTarget = mc.crosshairTarget;
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
        
        //switch
        ((IEMinecraftClient) mc).setWorldRenderer(newWorldRenderer);
        mc.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.update(0);
        helper.lightmapTexture.enable();
        BlockEntityRenderDispatcher.INSTANCE.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameMode.SPECTATOR);
        mc.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(newWorld.dimension.getType());
        ((IEParticleManager) mc.particleManager).mySetWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newWorld.dimension.getType()) {
            mc.crosshairTarget = BlockManipulationClient.remoteHitResult;
        }
        
        mc.getProfiler().push("render_portal_content");
        
        //invoke it!
        OFInterface.beforeRenderCenter.accept(partialTicks);
        mc.gameRenderer.renderWorld(
            partialTicks, getChunkUpdateFinishTime(),
            new MatrixStack()
        );
        OFInterface.afterRenderCenter.run();
        
        mc.getProfiler().pop();
        
        //recover
        ((IEMinecraftClient) mc).setWorldRenderer(oldWorldRenderer);
        mc.world = oldWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        mc.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) mc.particleManager).mySetWorld(oldWorld);
        mc.crosshairTarget = oldCrosshairTarget;
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        ((IECamera) mc.gameRenderer.getCamera()).resetState(oldCameraPos, oldWorld);
    }
    
    public void endCulling() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
    }
    
    public void startCulling() {
        //shaders do not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !OFInterface.isShaders.getAsBoolean()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public void updateCullingPlane(MatrixStack matrixStack) {
        McHelper.runWithTransformation(
            matrixStack,
            () -> {
                clipPlaneEquation = calcClipPlaneEquation();
                if (!OFInterface.isShaders.getAsBoolean()) {
                    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, clipPlaneEquation);
                }
            }
        );
    }
    
    private long getChunkUpdateFinishTime() {
        return 0;
    }
    
    //invoke this before rendering portal
    //its result depends on camra pos
    private double[] calcClipPlaneEquation() {
        if (FSRenderingContext.isRenderingScenery) {
            return FarSceneryRenderer.getCullingEquation();
        }
    
        Portal portal = CGlobal.renderer.getRenderingPortal();
    
        Vec3d planeNormal = portal.getContentDirection();
    
        Vec3d portalPos = portal.destination
            .subtract(portal.getContentDirection().multiply(0.01))//avoid z fighting
            .subtract(mc.gameRenderer.getCamera().getPos());
    
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x,
            planeNormal.y,
            planeNormal.z,
            c
        };
    }
    
    public double[] getClipPlaneEquation() {
        return clipPlaneEquation;
    }
    
    public void renderPlayerItself(Runnable doRenderEntity) {
        EntityRenderDispatcher entityRenderDispatcher =
            ((IEWorldRenderer) mc.worldRenderer).getEntityRenderDispatcher();
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        GameMode originalGameMode = MyRenderHelper.originalGameMode;
        
        Entity player = mc.cameraEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPos();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameMode oldGameMode = playerListEntry.getGameMode();
    
        McHelper.setPosAndLastTickPos(
            player, MyRenderHelper.originalPlayerPos, MyRenderHelper.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        doRenderEntity.run();
    
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
    
    public void resetFog() {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            return;
        }
    
        Camera camera = mc.gameRenderer.getCamera();
        float g = mc.gameRenderer.getViewDistance();
    
        Vec3d cameraPos = camera.getPos();
        double d = cameraPos.getX();
        double e = cameraPos.getY();
        double f = cameraPos.getZ();
    
        boolean bl2 = mc.world.dimension.isFogThick(
            MathHelper.floor(d),
            MathHelper.floor(e)
        ) || mc.inGameHud.getBossBarHud().shouldThickenFog();
        
        BackgroundRenderer.applyFog(
            camera,
            BackgroundRenderer.FogType.FOG_TERRAIN,
            Math.max(g - 16.0F, 32.0F),
            bl2
        );
    }
    
    //render fewer chunks when rendering portal
    //only active when graphic option is not fancy
    //NOTE we should not prune these chunks in setupTerrain()
    //because if it's pruned there these chunks will be rebuilt
    //then it will generate lag when player cross the portal by building chunks
    //we want the far chunks to be built but not rendered
    public void pruneVisibleChunksInFastGraphics(ObjectList<?> visibleChunks) {
        int renderDistance = mc.options.viewDistance;
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double range = ((renderDistance * 16) / 3) * ((renderDistance * 16) / 3);
        
        Predicate<ChunkBuilder.BuiltChunk> builtChunkPredicate = (builtChunk) -> {
            Vec3d center = builtChunk.boundingBox.getCenter();
            return center.squaredDistanceTo(cameraPos) > range;
        };
        
        pruneVisibleChunks(
            (ObjectList<Object>) visibleChunks,
            builtChunkPredicate
        );
    }
    
    public void pruneVisibleChunksForNearScenery(ObjectList<?> visibleChunks) {
        pruneVisibleChunks(
            ((ObjectList<Object>) visibleChunks),
            builtChunk -> !FaceRenderingTask.shouldRenderInNearScenery(builtChunk)
        );
    }
    
    private static void pruneVisibleChunks(
        ObjectList<Object> visibleChunks,
        Predicate<ChunkBuilder.BuiltChunk> builtChunkPredicate
    ) {
        Helper.removeIf(
            visibleChunks,
            obj -> builtChunkPredicate.test(((IEWorldRendererChunkInfo) obj).getBuiltChunk())
        );
    }
    
}
