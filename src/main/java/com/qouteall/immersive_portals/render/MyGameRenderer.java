package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.MyCommand;
import com.qouteall.immersive_portals.exposer.IEChunkRenderList;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.exposer.IEPlayerListEntry;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class MyGameRenderer {
    private MinecraftClient mc = MinecraftClient.getInstance();
    
    public MyGameRenderer() {
    
    }
    
    public void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld,
        Vec3d oldCameraPos
    ) {
        ChunkRenderDispatcher chunkRenderDispatcher =
            ((IEWorldRenderer) newWorldRenderer).getChunkRenderDispatcher();
        chunkRenderDispatcher.updateCameraPosition(
            mc.player.x, mc.player.z
        );
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) mc.gameRenderer;
        DimensionRenderHelper helper =
            Globals.clientWorldLoader.getDimensionRenderHelper(newWorld.dimension.getType());
        PlayerListEntry playerListEntry =
            MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(
                mc.player.getGameProfile().getId()
            );
    
        //store old state
        WorldRenderer oldWorldRenderer = mc.worldRenderer;
        ClientWorld oldWorld = mc.world;
        LightmapTextureManager oldLightmap = ieGameRenderer.getLightmapTextureManager();
        BackgroundRenderer oldFogRenderer = ieGameRenderer.getBackgroundRenderer();
        //assert BlockEntityRenderDispatcher.INSTANCE.world == oldWorld;
        GameMode oldGameMode = playerListEntry.getGameMode();
        boolean oldNoClip = mc.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        List oldChunkInfos = ((IEWorldRenderer) mc.worldRenderer).getChunkInfos();
        
        //switch
        mc.worldRenderer = newWorldRenderer;
        mc.world = newWorld;
        ieGameRenderer.setBackgroundRenderer(helper.fogRenderer);
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.update(0);
        helper.lightmapTexture.enable();
        BlockEntityRenderDispatcher.INSTANCE.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameMode.SPECTATOR);
        mc.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
    
        //this is important
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GuiLighting.disable();
        ((GameRenderer) ieGameRenderer).disableLightmap();
        //GlStateManager.disableAlphaTest();
        
        mc.getProfiler().push("render_portal_content");
    
        MyCommand.switchedFogRenderer = ieGameRenderer.getBackgroundRenderer();
        
        //invoke it!
        ieGameRenderer.renderCenter_(partialTicks, getChunkUpdateFinishTime());
        //mc.gameRenderer.renderWorld(partialTicks, getChunkUpdateFinishTime());
        
        mc.getProfiler().pop();
    
        //recover
        mc.worldRenderer = oldWorldRenderer;
        mc.world = oldWorld;
        ieGameRenderer.setBackgroundRenderer(oldFogRenderer);
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        mc.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        ((IEWorldRenderer) mc.worldRenderer).setChunkInfos(oldChunkInfos);
    
        restoreCameraPosOfRenderList(oldCameraPos);
    }
    
    private long getChunkUpdateFinishTime() {
        return 0;
    }
    
    public void restoreCameraPosOfRenderList(Vec3d oldCameraPos) {
        IEWorldRenderer worldRenderer = (IEWorldRenderer) mc.worldRenderer;
        IEChunkRenderList chunkRenderList = (IEChunkRenderList) worldRenderer.getChunkRenderList();
        chunkRenderList.setCameraPos(oldCameraPos.x, oldCameraPos.y, oldCameraPos.z);
    }
}
