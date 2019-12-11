package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.IEOFWorldRenderer;
import com.qouteall.immersive_portals.OFInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.optifine.Config;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.EXTFramebufferObject;

public class OFInterfaceInitializer {
    public static void init() {
        Validate.isTrue(OFInterface.isOptifinePresent);
        
        OFInterface.isShaders = Config::isShaders;
        OFInterface.isShadowPass = () -> Config.isShaders() && Shaders.isShadowPass;
        OFInterface.bindToShaderFrameBuffer = () -> {
            EXTFramebufferObject.glBindFramebufferEXT(36160, OFGlobal.getDfb.get());
            GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
        };
        OFInterface.beforeRenderCenter = (partialTicks) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
        
            Shaders.activeProgram = Shaders.ProgramNone;
            Shaders.beginRender(mc, mc.gameRenderer.getCamera(), partialTicks, 0);
        };
        OFInterface.afterRenderCenter = () -> Shaders.activeProgram = Shaders.ProgramNone;
        OFInterface.resetViewport = () -> {
            if (OFInterface.isShaders.getAsBoolean()) {
                GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
            }
        };
        OFInterface.onPlayerTraveled = (fromDimension1, toDimension1) -> {
            if (OFInterface.isShaders.getAsBoolean()) {
                OFGlobal.shaderContextManager.onPlayerTraveled(
                    fromDimension1,
                    toDimension1
                );
            }
        };
        OFInterface.shouldDisableFog = () -> {
            return Config.isFogOff() && MinecraftClient.getInstance().gameRenderer.fogStandard;
        };
        OFInterface.updateChunkRendererNeighbours = (dispatcher) -> {
            MinecraftClient.getInstance().getProfiler().push("neighbor");
        
            for (int j = 0; j < dispatcher.renderers.length; ++j) {
                ChunkRenderer renderChunk = dispatcher.renderers[j];
            
                for (int l = 0; l < Direction.ALL.length; ++l) {
                    Direction facing = Direction.ALL[l];
                    BlockPos posOffset16 = renderChunk.getNeighborPosition(facing);
                    ChunkRenderer neighbour = dispatcher.getChunkRenderer(posOffset16);
                    renderChunk.setRenderChunkNeighbour(facing, neighbour);
                }
            }
        
            MinecraftClient.getInstance().getProfiler().pop();
        };
        OFInterface.createNewRenderInfosNormal = newWorldRenderer1 -> {
            /**{@link WorldRenderer#chunkInfos}*/
            //in vanilla it will create new chunkInfos object every frame
            //but with optifine it will always use one object
            //we need to switch chunkInfos correctly
            //if we do not put it a new object, it will clear the original chunkInfos
            ((IEOFWorldRenderer) newWorldRenderer1).createNewRenderInfosNormal();
        };
        OFInterface.initShaderCullingManager = ShaderCullingManager::init;
    }
}
