package qouteall.imm_ptl.core.platform_specific;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPModMainClient;
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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.EntityType;
import net.minecraft.text.LiteralText;
import org.apache.commons.lang3.Validate;
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
            entityType -> EntityRendererRegistry.INSTANCE.register(
                entityType,
                (EntityRendererFactory) PortalEntityRenderer::new
            )
        );
        
        EntityRendererRegistry.INSTANCE.register(
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

//            IPGlobal.clientTaskList.addTask(MyTaskList.oneShotTask(() -> {
//                CHelper.printChat("[Immersive Portals] You are using Sodium with Immersive Portals." +
//                    "The compatibility is not yet stable.");
//            }));
        }
        else {
            Helper.log("Sodium is not present");
        }
        
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            Helper.log("Iris is present");
            IrisInterface.invoker = new IrisInterface.OnIrisPresent();

//            IPGlobal.clientTaskList.addTask(MyTaskList.oneShotTask(() -> {
//                CHelper.printChat("[Immersive Portals] You are using Iris with Immersive Portals." +
//                    "The compatibility is not yet stable. Cross-portal entity rendering is being disabled.");
//            }));
        }
        else {
            Helper.log("Iris is not present");
        }
        
        initWarnings();
    }
    
    
    private static boolean checked = false;
    
    private static void initWarnings() {
        IPGlobal.postClientTickSignal.connect(() -> {
            if (MinecraftClient.getInstance().world == null) {
                return;
            }
            
            if (checked) {
                return;
            }
            
            if (FabricLoader.getInstance().isModLoaded("canvas")) {
                CHelper.printChat(new LiteralText(
                    "[Immersive Portals] Warning: Canvas is incompatible with Immersive Portals."
                ));
            }
            
            checked = true;
        });
    }
    
}
