package qouteall.imm_ptl.core;

import com.mojang.blaze3d.platform.GlDebugInfo;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.option.GraphicsMode;
import qouteall.imm_ptl.core.commands.ClientDebugCommand;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisCompatibilityPortalRenderer;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisPortalRenderer;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.RendererUsingFrameBuffer;
import qouteall.imm_ptl.core.render.RendererUsingStencil;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.optimization.GLResourceCache;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.TranslatableText;

import java.util.UUID;

public class IPModMainClient {
    
    private static boolean fabulousWarned = false;
    
    public static void switchToCorrectRenderer() {
        if (PortalRendering.isRendering()) {
            //do not switch when rendering
            return;
        }
        
        if (MinecraftClient.getInstance().options.graphicsMode == GraphicsMode.FABULOUS) {
            if (!fabulousWarned) {
                fabulousWarned = true;
                CHelper.printChat(new TranslatableText("imm_ptl.fabulous_warning"));
            }
        }
        
        if (IrisInterface.invoker.isIrisPresent()) {
            if (IrisInterface.invoker.isShaders()) {
                switch (IPGlobal.renderMode) {
                    case normal -> switchRenderer(IrisPortalRenderer.instance);
                    case compatibility -> switchRenderer(IrisCompatibilityPortalRenderer.instance);
                    case debug -> switchRenderer(IrisCompatibilityPortalRenderer.debugModeInstance);
                    case none -> switchRenderer(IPCGlobal.rendererDummy);
                }
                return;
            }
        }
        
        switch (IPGlobal.renderMode) {
            case normal -> switchRenderer(IPCGlobal.rendererUsingStencil);
            case compatibility -> switchRenderer(IPCGlobal.rendererUsingFrameBuffer);
            case debug -> switchRenderer(IPCGlobal.rendererDebug);
            case none -> switchRenderer(IPCGlobal.rendererDummy);
        }
        
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (IPCGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            IPCGlobal.renderer = renderer;
        }
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
    
    private static void showIntelVideoCardWarning() {
        MinecraftClient.getInstance().execute(() -> {
            if (GlDebugInfo.getVendor().toLowerCase().contains("intel")) {
                CHelper.printChat(new TranslatableText("imm_ptl.intel_warning"));
            }
        });
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
        
        SharedBlockMeshBuffers.init();
        
        GcMonitor.initClient();
        
        ClientDebugCommand.register(ClientCommandManager.DISPATCHER);
        
        showPreviewWarning();
        
        showIntelVideoCardWarning();
    }
    
}
