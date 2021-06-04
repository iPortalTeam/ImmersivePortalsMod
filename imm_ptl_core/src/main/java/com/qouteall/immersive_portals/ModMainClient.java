package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.miscellaneous.GcMonitor;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.optifine_compatibility.OFBuiltChunkStorageFix;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFInterfaceInitializer;
import com.qouteall.immersive_portals.portal.PortalRenderInfo;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.RendererUsingFrameBuffer;
import com.qouteall.immersive_portals.render.RendererUsingStencil;
import com.qouteall.immersive_portals.render.ShaderCodeTransformation;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.render.context_management.CloudContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.lag_spike_fix.GlBufferCache;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.TranslatableText;

import java.util.UUID;

public class ModMainClient {
    
    public static void switchToCorrectRenderer() {
        if (PortalRendering.isRendering()) {
            //do not switch when rendering
            return;
        }
        if (OFInterface.isShaders.getAsBoolean()) {
            switch (Global.renderMode) {
                case normal:
                    switchRenderer(OFGlobal.rendererMixed);
                    break;
                case compatibility:
                    switchRenderer(OFGlobal.rendererDeferred);
                    break;
                case debug:
                    switchRenderer(OFGlobal.rendererDebugWithShader);
                    break;
                case none:
                    switchRenderer(CGlobal.rendererDummy);
                    break;
            }
        }
        else {
            switch (Global.renderMode) {
                case normal:
                    switchRenderer(CGlobal.rendererUsingStencil);
                    break;
                case compatibility:
                    switchRenderer(CGlobal.rendererUsingFrameBuffer);
                    break;
                case debug:
                    switchRenderer(CGlobal.rendererDebug);
                    break;
                case none:
                    switchRenderer(CGlobal.rendererDummy);
                    break;
            }
        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (CGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            CGlobal.renderer = renderer;
        }
    }
    
    private static void showOptiFineWarning() {
        ModMain.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> MinecraftClient.getInstance().world == null,
            MyTaskList.oneShotTask(() -> {
                MinecraftClient.getInstance().inGameHud.addChatMessage(
                    MessageType.CHAT,
                    new TranslatableText("imm_ptl.optifine_warning"),
                    UUID.randomUUID()
                );
            })
        ));
    }
    
    public static void init() {
        MyNetworkClient.init();
        
        ClientWorldLoader.init();
        
        MinecraftClient.getInstance().execute(() -> {
            CGlobal.rendererUsingStencil = new RendererUsingStencil();
            CGlobal.rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
            
            CGlobal.renderer = CGlobal.rendererUsingStencil;
            CGlobal.clientTeleportationManager = new ClientTeleportationManager();
            
            if (CGlobal.shaderManager == null) {
                CGlobal.shaderManager = new ShaderManager();
            }
            
            ShaderCodeTransformation.init();
        });
        
        O_O.loadConfigFabric();
        
        DubiousThings.init();
        
        CrossPortalEntityRenderer.init();
        
        GlBufferCache.init();
        
        CollisionHelper.initClient();
        
        PortalRenderInfo.init();
        
        CloudContext.init();
        
        OFInterface.isOptifinePresent = O_O.detectOptiFine();
        if (OFInterface.isOptifinePresent) {
            OFInterfaceInitializer.init();
            OFBuiltChunkStorageFix.init();
            showOptiFineWarning();
        }
        
        GcMonitor.initClient();
        
        MyRenderHelper.init();
        
        Helper.log(OFInterface.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
    }
    
}
