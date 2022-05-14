package qouteall.imm_ptl.core.compat.mixin;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.render.FrustumCuller;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class MixinSodiumWorldRenderer {
    @Inject(
        method = "updateChunks",
        at = @At("HEAD")
    )
    private void onUpdateChunks(Camera camera, Frustum frustum, int frame, boolean spectator, CallbackInfo ci) {
        SodiumInterface.frustumCuller = new FrustumCuller();
        Vec3 cameraPos = camera.getPosition();
        SodiumInterface.frustumCuller.update(cameraPos.x, cameraPos.y, cameraPos.z);
    }
}
