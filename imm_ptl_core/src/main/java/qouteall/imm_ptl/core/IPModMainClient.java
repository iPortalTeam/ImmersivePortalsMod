package qouteall.imm_ptl.core;

import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import qouteall.imm_ptl.core.commands.ClientDebugCommand;
import qouteall.imm_ptl.core.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.iris_compatibility.IrisPortalRenderer;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;
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
import qouteall.imm_ptl.core.render.lag_spike_fix.GLResourceCache;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.TranslatableText;

import java.util.UUID;

public class IPModMainClient {
    
    public static void switchToCorrectRenderer() {
        if (PortalRendering.isRendering()) {
            //do not switch when rendering
            return;
        }
        if (IrisInterface.invoker.isIrisPresent()) {
            if (IrisInterface.invoker.isShaders()) {
                switchRenderer(IrisPortalRenderer.instance);
                return;
            }
        }
        if (OFInterface.isShaders.getAsBoolean()) {
            switch (IPGlobal.renderMode) {
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
                    switchRenderer(IPCGlobal.rendererDummy);
                    break;
            }
        }
        else {
            switch (IPGlobal.renderMode) {
                case normal:
                    switchRenderer(IPCGlobal.rendererUsingStencil);
                    break;
                case compatibility:
                    switchRenderer(IPCGlobal.rendererUsingFrameBuffer);
                    break;
                case debug:
                    switchRenderer(IPCGlobal.rendererDebug);
                    break;
                case none:
                    switchRenderer(IPCGlobal.rendererDummy);
                    break;
            }
        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (IPCGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            IPCGlobal.renderer = renderer;
        }
    }
    
    private static void showOptiFineWarning() {
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
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
    
    private static void showPreviewWarning() {
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> MinecraftClient.getInstance().world == null,
            MyTaskList.oneShotTask(() -> {
                MinecraftClient.getInstance().inGameHud.addChatMessage(
                    MessageType.CHAT,
                    new TranslatableText("imm_ptl.preview_warning").append(
                        McHelper.getLinkText("https://github.com/qouteall/ImmersivePortalsMod/issues")
                    ),
                    UUID.randomUUID()
                );
            })
        ));
    }
    
    public static void init() {
        IPNetworkingClient.init();
        
        ClientWorldLoader.init();
        
        MinecraftClient.getInstance().execute(() -> {
            ShaderCodeTransformation.init();
            
            MyRenderHelper.init();
            
            IPCGlobal.rendererUsingStencil = new RendererUsingStencil();
            IPCGlobal.rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
            
            IPCGlobal.renderer = IPCGlobal.rendererUsingStencil;
            IPCGlobal.clientTeleportationManager = new ClientTeleportationManager();
        });
        
        O_O.loadConfigFabric();
        
        DubiousThings.init();
        
        CrossPortalEntityRenderer.init();
        
        GLResourceCache.init();
        
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
        
        showPreviewWarning();
        
        Helper.log(OFInterface.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
    }
    
}
