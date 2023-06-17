package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.QueryManager;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.q_misc_util.Helper;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenOverlay {
    @Inject(method = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;getSystemInformation()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> returnValue = cir.getReturnValue();
        returnValue.add("Rendered Portals: " + RenderStates.lastPortalRenderInfos.size());
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Portal collidingPortal = ((IEEntity) player).ip_getCollidingPortal();
            if (collidingPortal != null) {
                String text = "Colliding " + collidingPortal.toString();
                returnValue.addAll(Helper.splitStringByLen(text, 50));
            }
        }
        
        returnValue.add("Occlusion Query Stall: " + QueryManager.queryStallCounter);
        returnValue.add("Client Perf %s %d %d".formatted(
            ClientPerformanceMonitor.level,
            ClientPerformanceMonitor.getAverageFps(),
            ClientPerformanceMonitor.getAverageFreeMemoryMB()
        ));
        
        String sharedMeshBufferDebug = SharedBlockMeshBuffers.getDebugString();
        if (sharedMeshBufferDebug != null) {
            returnValue.add(sharedMeshBufferDebug);
        }
        
        returnValue.add(StableClientTimer.getDebugString());
        
        String blockPointingInfo = BlockManipulationClient.getDebugString();
        if (blockPointingInfo != null) {
            returnValue.add(blockPointingInfo);
        }
        
        if (RenderStates.debugText != null && !RenderStates.debugText.isEmpty()) {
            returnValue.addAll(Helper.splitStringByLen(
                "Debug: " + RenderStates.debugText,
                50
            ));
        }
    }
}
