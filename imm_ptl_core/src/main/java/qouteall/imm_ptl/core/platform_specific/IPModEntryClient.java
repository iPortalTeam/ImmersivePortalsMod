package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.IPModMainClient;
import qouteall.imm_ptl.core.compat.IPModInfoChecking;
import qouteall.imm_ptl.core.compat.iris_compatibility.ExperimentalIrisPortalRenderer;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.portal.BreakableMirror;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;
import qouteall.imm_ptl.core.portal.Mirror;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalTrackedPortal;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.imm_ptl.core.portal.global_portals.WorldWrappingPortal;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;
import qouteall.imm_ptl.core.render.LoadingIndicatorRenderer;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.Arrays;

public class IPModEntryClient implements ClientModInitializer {
    
    public static void initPortalRenderers() {
        
        Arrays.stream(new EntityType<?>[]{
            Portal.entityType,
            NetherPortalEntity.entityType,
            EndPortalEntity.entityType,
            Mirror.entityType,
            BreakableMirror.entityType,
            GlobalTrackedPortal.entityType,
            WorldWrappingPortal.entityType,
            VerticalConnectingPortal.entityType,
            GeneralBreakablePortal.entityType
        }).peek(
            Validate::notNull
        ).forEach(
            entityType -> EntityRendererRegistry.register(
                entityType,
                (EntityRendererProvider) PortalEntityRenderer::new
            )
        );
        
        EntityRendererRegistry.register(
            LoadingIndicatorEntity.entityType,
            LoadingIndicatorRenderer::new
        );
        
    }
    
    @Override
    public void onInitializeClient() {
        IPModMainClient.init();
        
        initPortalRenderers();
        
        boolean isSodiumPresent =
            FabricLoader.getInstance().isModLoaded("sodium");
        if (isSodiumPresent) {
            Helper.log("Sodium is present");
            
            SodiumInterface.invoker = new SodiumInterface.OnSodiumPresent();
            
            IPGlobal.clientTaskList.addTask(MyTaskList.oneShotTask(() -> {
                if (IPGlobal.enableWarning) {
                    CHelper.printChat(
                        Component.translatable("imm_ptl.sodium_warning")
                            .append(IPMcHelper.getDisableWarningText())
                    );
                }
            }));
        }
        else {
            Helper.log("Sodium is not present");
        }
        
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            Helper.log("Iris is present");
            IrisInterface.invoker = new IrisInterface.OnIrisPresent();
            ExperimentalIrisPortalRenderer.init();
            
            IPGlobal.clientTaskList.addTask(MyTaskList.oneShotTask(() -> {
                if (IPGlobal.enableWarning) {
                    CHelper.printChat(
                        Component.translatable("imm_ptl.iris_warning")
                            .append(IPMcHelper.getDisableWarningText())
                    );
                }
            }));
        }
        else {
            Helper.log("Iris is not present");
        }
        
        IPModInfoChecking.initClient();
    }
    
}
