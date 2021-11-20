package qouteall.imm_ptl.core.mixin.client;

import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.QueryManager;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class MixinDebugHud {
    @Inject(method = "getRightText", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> returnValue = cir.getReturnValue();
        returnValue.add("Rendered Portals: " + RenderStates.lastPortalRenderInfos.size());
        
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            Portal collidingPortal = ((IEEntity) player).getCollidingPortal();
            if (collidingPortal != null) {
                String text = "Colliding " + collidingPortal.toString();
                returnValue.addAll(Helper.splitStringByLen(text, 50));
            }
        }
        
        returnValue.add("Occlusion Query Stall: " + QueryManager.queryStallCounter);
        returnValue.add("Client Perf %s %d %d".formatted(
            ClientPerformanceMonitor.currentPerformanceLevel,
            ClientPerformanceMonitor.getAverageFps(),
            ClientPerformanceMonitor.getAverageFreeMemoryMB()
        ));
        
        if (RenderStates.debugText != null && !RenderStates.debugText.isEmpty()) {
            returnValue.addAll(Helper.splitStringByLen(
                "Debug: " + RenderStates.debugText,
                50
            ));
        }
    }
}
