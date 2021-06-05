package qouteall.imm_ptl.core;

import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import qouteall.imm_ptl.core.commands.ClientDebugCommand;
import qouteall.imm_ptl.core.platform_specific.MyNetworkClient;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.my_util.MyTaskList;
import qouteall.imm_ptl.core.optifine_compatibility.OFBuiltChunkStorageFix;
import qouteall.imm_ptl.core.optifine_compatibility.OFGlobal;
import qouteall.imm_ptl.core.optifine_compatibility.OFInterfaceInitializer;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.RendererUsingFrameBuffer;
import qouteall.imm_ptl.core.render.RendererUsingStencil;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.lag_spike_fix.GlBufferCache;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
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
            ShaderCodeTransformation.init();
            
            MyRenderHelper.init();
            
            CGlobal.rendererUsingStencil = new RendererUsingStencil();
            CGlobal.rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
            
            CGlobal.renderer = CGlobal.rendererUsingStencil;
            CGlobal.clientTeleportationManager = new ClientTeleportationManager();
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
    
        ClientDebugCommand.register(ClientCommandManager.DISPATCHER);
        
        Helper.log(OFInterface.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
    }
    
}
