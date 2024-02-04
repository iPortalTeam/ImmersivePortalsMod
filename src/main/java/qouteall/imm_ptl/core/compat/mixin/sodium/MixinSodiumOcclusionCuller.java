package qouteall.imm_ptl.core.compat.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(OcclusionCuller.class)
public abstract class MixinSodiumOcclusionCuller {
    @Shadow(remap = false)
    protected abstract RenderSection getRenderSection(int x, int y, int z);
    
    @Shadow
    public static boolean isWithinFrustum(Viewport viewport, RenderSection section) {
        throw new RuntimeException();
    }
    
    @Unique
    private @Nullable SectionPos ip_modifiedStartPoint;
    
    @Unique
    private static boolean ip_tolerantInitialFrustumTestFail;
    
    // update the iteration start point modification value
    @SuppressWarnings("ConstantValue")
    @ModifyVariable(
        method = "findVisible", at = @At("HEAD"), argsOnly = true, remap = false
    )
    boolean modifyUseOcclusionCulling(
        boolean originalValue,
        OcclusionCuller.Visitor visitor, Viewport viewport, float searchDistance, boolean useOcclusionCulling, int frame
    ) {
        boolean doUseOcclusionCulling = PortalRendering.shouldEnableSodiumCaveCulling();
        
        ip_modifiedStartPoint = null;
        ip_tolerantInitialFrustumTestFail = false;
        
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            if (renderingPortal instanceof Portal portal) {
                Vec3 cameraPos = CHelper.getCurrentCameraPos();
                ip_modifiedStartPoint = portal.getPortalShape().getModifiedVisibleSectionIterationOrigin(
                    portal, cameraPos
                );
                if (ip_modifiedStartPoint != null) {
                    doUseOcclusionCulling = false;
                    
                    RenderSection renderSection = getRenderSection(
                        ip_modifiedStartPoint.x(), ip_modifiedStartPoint.y(), ip_modifiedStartPoint.z()
                    );
                    if (renderSection != null && !isWithinFrustum(viewport, renderSection)) {
                        ip_tolerantInitialFrustumTestFail = true;
                    }
                }
            }
        }
        
        return doUseOcclusionCulling;
    }
    
    // apply start point modification
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;",
            remap = true
        ),
        remap = false
    )
    private SectionPos redirectGetChunkCoordInInit(Viewport instance) {
        if (ip_modifiedStartPoint != null) {
            return ip_modifiedStartPoint;
        }
        
        return instance.getChunkCoord();
    }
    
    // apply start point modification
    @Redirect(
        method = "initWithinWorld",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"
        ),
        remap = false
    )
    private SectionPos redirectGetChunkCoordInInitWithinWorld(Viewport instance) {
        if (ip_modifiedStartPoint != null) {
            return ip_modifiedStartPoint;
        }
        
        return instance.getChunkCoord();
    }
    
    // when iteration start point become a position that's outside of frustum
    // make it tolerant early frustum test failures to avoid wrongly halting iteration
    @Inject(
        method = "isWithinFrustum", at = @At("RETURN"), cancellable = true,
        remap = false
    )
    private static void onIsOutsideFrustum(
        Viewport viewport, RenderSection section,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (ip_tolerantInitialFrustumTestFail) {
            boolean withinFrustum = cir.getReturnValueZ();
            if (withinFrustum) {
                // when found a section that's in frustum, frustum test become normal
                ip_tolerantInitialFrustumTestFail = false;
            }
            cir.setReturnValue(true);
        }
    }
}
