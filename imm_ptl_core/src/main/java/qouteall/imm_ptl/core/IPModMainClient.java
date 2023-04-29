package qouteall.imm_ptl.core;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import qouteall.imm_ptl.core.commands.ClientDebugCommand;
import qouteall.imm_ptl.core.compat.IPFlywheelCompat;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.miscellaneous.DubiousThings;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.network.IPNetworkingClient;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.animation.ClientPortalAnimationManagement;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.RendererUsingFrameBuffer;
import qouteall.imm_ptl.core.render.RendererUsingStencil;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.optimization.GLResourceCache;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.imm_ptl.core.collision.CollisionHelper;
import qouteall.q_misc_util.my_util.MyTaskList;

public class IPModMainClient {
    
    private static final boolean isPreview = false;
    
    private static void showPreviewWarning() {
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPConfig.getConfig().shouldDisplayWarning("preview")) {
                    MutableComponent text = Component.translatable("imm_ptl.preview_warning").append(
                        McHelper.getLinkText(O_O.getIssueLink())
                    );
                    
                    CHelper.printChat(text);
                }
            })
        ));
    }

//    private static void showIntelVideoCardWarning() {
//        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
//            () -> Minecraft.getInstance().level == null,
//            MyTaskList.oneShotTask(() -> {
//                if (GlUtil.getVendor().toLowerCase().contains("intel")) {
//                    CHelper.printChat(Component.translatable("imm_ptl.intel_warning"));
//                }
//            })
//        ));
//    }
    
    private static void showNvidiaVideoCardWarning() {
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPMcHelper.isNvidiaVideocard()) {
                    if (!SodiumInterface.invoker.isSodiumPresent()) {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.nvidia_warning")
                                .withStyle(ChatFormatting.RED)
                                .append(McHelper.getLinkText("https://github.com/CaffeineMC/sodium-fabric/issues/1486"))
                        );
                    }
                }
            })
        ));
    }
    
    public static void init() {
        IPNetworkingClient.init();
        
        ClientWorldLoader.init();
        
        Minecraft.getInstance().execute(() -> {
            ShaderCodeTransformation.init();
            
            MyRenderHelper.init();
            
            IPCGlobal.rendererUsingStencil = new RendererUsingStencil();
            IPCGlobal.rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
            
            IPCGlobal.renderer = IPCGlobal.rendererUsingStencil;
            IPCGlobal.clientTeleportationManager = new ClientTeleportationManager();
        });
        
        DubiousThings.init();
        
        CrossPortalEntityRenderer.init();
        
        GLResourceCache.init();
        
        CollisionHelper.initClient();
        
        PortalRenderInfo.init();
        
        CloudContext.init();
        
        SharedBlockMeshBuffers.init();
        
        GcMonitor.initClient();
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ClientDebugCommand.register(dispatcher);
        });
        
        if (isPreview) {
            showPreviewWarning();
        }

//        showIntelVideoCardWarning();
        
        showNvidiaVideoCardWarning();
        
        StableClientTimer.init();
        
        ClientPortalAnimationManagement.init();
        
        VisibleSectionDiscovery.init();
        
        MyBuiltChunkStorage.init();
        
        IPFlywheelCompat.init();
    }
    
}
