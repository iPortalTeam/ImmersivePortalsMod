package qouteall.imm_ptl.core.compat.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;

@Mixin(value = Viewport.class, remap = false)
public class MixinSodiumViewport {
    @Redirect(
        method = "isBoxVisible",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/viewport/frustum/Frustum;testAab(FFFFFF)Z"
        )
    )
    private boolean redirectTestAab(
        Frustum instance,
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
    ) {
        boolean inFrustum = instance.testAab(
            minX, minY, minZ, maxX, maxY, maxZ
        );
        
        if (inFrustum) {
            if (SodiumInterface.frustumCuller != null) {
                boolean canDetermineInvisible =
                    SodiumInterface.frustumCuller.canDetermineInvisibleWithCameraCoord(
                        minX, minY, minZ, maxX, maxY, maxZ
                    );
                return !canDetermineInvisible;
            }
        }
        
        return inFrustum;
    }
}
