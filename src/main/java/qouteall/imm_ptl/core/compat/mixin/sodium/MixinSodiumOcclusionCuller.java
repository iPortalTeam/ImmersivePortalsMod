package qouteall.imm_ptl.core.compat.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(OcclusionCuller.class)
public class MixinSodiumOcclusionCuller {
    @Unique
    private @Nullable SectionPos ip_modifiedStartPoint;
    
    // update the iteration start point modification value
    @ModifyVariable(
        method = "findVisible", at = @At("HEAD"), argsOnly = true, remap = false
    )
    boolean modifyUseOcclusionCulling(boolean originalValue) {
        boolean newValue = PortalRendering.shouldEnableSodiumCaveCulling();
        
        ip_modifiedStartPoint = null;
        
        PortalLike renderingPortal = PortalRendering.getRenderingPortal();
        if (renderingPortal instanceof Portal portal) {
            ip_modifiedStartPoint = portal.getPortalShape().getModifiedVisibleSectionIterationOrigin(
                portal, CHelper.getCurrentCameraPos()
            );
        }
        
        return newValue;
    }
    
    // apply start point modification
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/viewport/Viewport;getChunkCoord()Lnet/minecraft/core/SectionPos;"
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
}
