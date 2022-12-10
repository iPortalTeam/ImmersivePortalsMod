package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.render.context_management.*;
import qouteall.q_misc_util.my_util.LimitedLogger;

import javax.annotation.Nullable;
import java.util.Stack;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class MyGameRenderer {
    public static final Minecraft client = Minecraft.getInstance();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    // portal rendering and outer world rendering uses different buffer builder storages
    private static Stack<RenderBuffers> secondaryRenderBuffers = new Stack<>();
    private static int usingRenderBuffersObjectNum = 0;
    
    // the vanilla visibility sections discovery code is multi-threaded
    // when the player teleports through a portal, on the first frame it will not work normally
    // so use IP's non-multi-threaded algorithm at the first frame
    public static int vanillaTerrainSetupOverride = 0;
    
    public static boolean enablePortalCaveCulling = true;
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(() -> {
            secondaryRenderBuffers.clear();
        });
    }
    
    @Nullable
    private static RenderBuffers acquireRenderBuffersObject() {
        if (usingRenderBuffersObjectNum >= 2) {
            return null;
        }
        usingRenderBuffersObjectNum--;
        
        if (secondaryRenderBuffers.isEmpty()) {
            return new RenderBuffers();
        }
        else {
            return secondaryRenderBuffers.pop();
        }
    }
    
    private static void returnRenderBuffersObject(RenderBuffers renderBuffers) {
        usingRenderBuffersObjectNum++;
        secondaryRenderBuffers.push(renderBuffers);
    }
    
    public static void renderWorldNew(
        WorldRenderInfo worldRenderInfo,
        Consumer<Runnable> invokeWrapper
    ) {
        WorldRenderInfo.pushRenderInfo(worldRenderInfo);
        
        switchAndRenderTheWorld(
            worldRenderInfo.world,
            worldRenderInfo.cameraPos,
            worldRenderInfo.cameraPos,
            invokeWrapper,
            worldRenderInfo.renderDistance,
            worldRenderInfo.doRenderHand
        );
        
        WorldRenderInfo.popRenderInfo();
    }
    
    private static void switchAndRenderTheWorld(
        ClientLevel newWorld,
        Vec3 thisTickCameraPos,
        Vec3 lastTickCameraPos,
        Consumer<Runnable> invokeWrapper,
        int renderDistance,
        boolean doRenderHand
    ) {
        if (!enablePortalCaveCulling) {
            client.smartCull = false;
        }
        
        if (!PortalRendering.shouldEnableSodiumCaveCulling()) {
            client.smartCull = false;
        }
        
        Entity cameraEntity = client.cameraEntity;
        
        Vec3 oldEyePos = McHelper.getEyePos(cameraEntity);
        Vec3 oldLastTickEyePos = McHelper.getLastTickEyePos(cameraEntity);
        
        ResourceKey<Level> oldEntityDimension = cameraEntity.level.dimension();
        ClientLevel oldEntityWorld = ((ClientLevel) cameraEntity.level);
        
        ResourceKey<Level> newDimension = newWorld.dimension();
        
        //switch the camera entity pos
        McHelper.setEyePos(cameraEntity, thisTickCameraPos, lastTickCameraPos);
        cameraEntity.level = newWorld;
        
        LevelRenderer worldRenderer = ClientWorldLoader.getWorldRenderer(newDimension);
        
        CHelper.checkGlError();
        
        float tickDelta = RenderStates.tickDelta;
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
        DimensionRenderHelper helper =
            ClientWorldLoader.getDimensionRenderHelper(newDimension);
        Camera newCamera = new Camera();
        
        //store old state
        LevelRenderer oldWorldRenderer = client.levelRenderer;
        LightTexture oldLightmap = client.gameRenderer.lightTexture();
        boolean oldNoClip = client.player.noPhysics;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        ObjectArrayList<LevelRenderer.RenderChunkInfo> oldChunkInfoList =
            ((IEWorldRenderer) oldWorldRenderer).portal_getChunkInfoList();
        HitResult oldCrosshairTarget = client.hitResult;
        Camera oldCamera = client.gameRenderer.getMainCamera();
        PostChain oldTransparencyShader = ((IEWorldRenderer) worldRenderer).portal_getTransparencyShader();
        RenderBuffers oldRenderBuffers = ((IEWorldRenderer) worldRenderer).ip_getRenderBuffers();
        RenderBuffers oldClientRenderBuffers = client.renderBuffers();
        Frustum oldFrustum = ((IEWorldRenderer) worldRenderer).portal_getFrustum();
        
        // the projection matrix contains view bobbing.
        // the view bobbing is related with scale
        Matrix4f oldProjectionMatrix = RenderSystem.getProjectionMatrix();
        
        ObjectArrayList<LevelRenderer.RenderChunkInfo> newChunkInfoList = VisibleSectionDiscovery.takeList();
        ((IEWorldRenderer) oldWorldRenderer).portal_setChunkInfoList(newChunkInfoList);
        
        Object irisPipeline = IrisInterface.invoker.getPipeline(worldRenderer);
        
        //switch
        ((IEMinecraftClient) client).setWorldRenderer(worldRenderer);
        client.level = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        
        client.getBlockEntityRenderDispatcher().level = newWorld;
        client.player.noPhysics = true;
        client.gameRenderer.setRenderHand(doRenderHand);
        
        FogRendererContext.swappingManager.pushSwapping(newDimension);
        ((IEParticleManager) client.particleEngine).ip_setWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newDimension) {
            client.hitResult = BlockManipulationClient.remoteHitResult;
        }
        ieGameRenderer.setCamera(newCamera);
        
        RenderBuffers newRenderBuffers = null;
        if (IPGlobal.useSecondaryEntityVertexConsumer) {
            newRenderBuffers = acquireRenderBuffersObject();
            ((IEWorldRenderer) worldRenderer).ip_setRenderBuffers(newRenderBuffers);
            ((IEMinecraftClient) client).ip_setRenderBuffers(newRenderBuffers);
        }
        
        Object newSodiumContext = SodiumInterface.invoker.createNewContext(renderDistance);
        SodiumInterface.invoker.switchContextWithCurrentWorldRenderer(newSodiumContext);
        
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(null);
        
        IrisInterface.invoker.setPipeline(worldRenderer, null);
        
        //update lightmap
        if (!RenderStates.isDimensionRendered(newDimension)) {
            helper.lightmapTexture.updateLightTexture(0);
        }
        
        //invoke rendering
        try {
            invokeWrapper.accept(() -> {
                client.getProfiler().push("render_portal_content");
                client.gameRenderer.renderLevel(
                    tickDelta,
                    Util.getNanos(),
                    new PoseStack()
                );
                client.getProfiler().pop();
            });
        } catch (Throwable e) {
            limitedLogger.invoke(e::printStackTrace);
        }
        
        SodiumInterface.invoker.switchContextWithCurrentWorldRenderer(newSodiumContext);
        
        //recover
        
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        client.level = oldEntityWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        client.getBlockEntityRenderDispatcher().level = oldEntityWorld;
        client.player.noPhysics = oldNoClip;
        client.gameRenderer.setRenderHand(oldDoRenderHand);
        
        ((IEParticleManager) client.particleEngine).ip_setWorld(oldEntityWorld);
        client.hitResult = oldCrosshairTarget;
        ieGameRenderer.setCamera(oldCamera);
        
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(oldTransparencyShader);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).portal_setChunkInfoList(oldChunkInfoList);
        VisibleSectionDiscovery.returnList(newChunkInfoList);
        
        ((IEWorldRenderer) worldRenderer).ip_setRenderBuffers(oldRenderBuffers);
        ((IEMinecraftClient) client).ip_setRenderBuffers(oldClientRenderBuffers);
        if (newRenderBuffers != null) {
            returnRenderBuffersObject(newRenderBuffers);
        }
        
        ((IEWorldRenderer) worldRenderer).portal_setFrustum(oldFrustum);
        
        RenderSystem.setProjectionMatrix(oldProjectionMatrix);
        
        IrisInterface.invoker.setPipeline(worldRenderer, irisPipeline);
        
        client.getEntityRenderDispatcher()
            .prepare(
                client.level,
                oldCamera,
                client.crosshairPickEntity
            );
        
        CHelper.checkGlError();
        
        //restore the camera entity pos
        cameraEntity.level = oldEntityWorld;
        McHelper.setEyePos(cameraEntity, oldEyePos, oldLastTickEyePos);
        
        client.smartCull = true;
    }
    
    public static void renderPlayerItself(Runnable doRenderEntity) {
        EntityRenderDispatcher entityRenderDispatcher =
            ((IEWorldRenderer) client.levelRenderer).ip_getEntityRenderDispatcher();
        PlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        GameType originalGameMode = RenderStates.originalGameMode;
        
        Entity player = client.cameraEntity;
        assert player != null;
        
        Vec3 oldPos = player.position();
        Vec3 oldLastTickPos = McHelper.lastTickPosOf(player);
        GameType oldGameMode = playerListEntry.getGameMode();
        
        McHelper.setPosAndLastTickPos(
            player, RenderStates.originalPlayerPos, RenderStates.originalPlayerLastTickPos
        );
        
        doRenderEntity.run();
        
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
    }
    
    @IPVanillaCopy
    public static void resetFogState() {
        Camera camera = client.gameRenderer.getMainCamera();
        float g = client.gameRenderer.getRenderDistance();
        
        Vec3 cameraPos = camera.getPosition();
        double d = cameraPos.x();
        double e = cameraPos.y();
        double f = cameraPos.z();
        
        boolean bl2 = client.level.effects().isFoggyAt(Mth.floor(d), Mth.floor(e)) ||
            client.gui.getBossOverlay().shouldCreateWorldFog();
        
        boolean bl3 = client.level.effects().isFoggyAt(Mth.floor(d), Mth.floor(e)) || client.gui.getBossOverlay().shouldCreateWorldFog();
        
        FogRenderer.setupFog(
            camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(g, 32.0F), bl3, RenderStates.tickDelta
        );
        FogRenderer.levelFogColor();
    }
    
    public static void updateFogColor() {
        FogRenderer.setupColor(
            client.gameRenderer.getMainCamera(),
            RenderStates.tickDelta,
            client.level,
            client.options.getEffectiveRenderDistance(),
            client.gameRenderer.getDarkenWorldAmount(RenderStates.tickDelta)
        );
    }
    
    public static void resetDiffuseLighting(PoseStack matrixStack) {
        if (client.level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel(matrixStack.last().pose());
        }
        else {
            Lighting.setupLevel(matrixStack.last().pose());
        }
    }
    
    
}
