package qouteall.imm_ptl.peripheral.mixin.client.alternate_dimension;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;

@Mixin(FogRenderer.class)
public class MixinFogRenderer_A_CVB {
    //avoid alternate dimension dark when seeing from overworld
    @Redirect(
        method = "computeFogColor",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;position()Lnet/minecraft/world/phys/Vec3;"
        ),
        require = 0,
        expect = 0
    )
    private static Vec3 redirectCameraGetPos(Camera camera) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null && AlternateDimensions.isAlternateDimension(world)) {
            return new Vec3(
                camera.position().x,
                Math.max(32.0, camera.position().y),
                camera.position().z
            );
        }
        else {
            return camera.position();
        }
    }
}
