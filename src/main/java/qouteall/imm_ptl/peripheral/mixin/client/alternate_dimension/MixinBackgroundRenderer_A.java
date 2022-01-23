package qouteall.imm_ptl.peripheral.mixin.client.alternate_dimension;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer_A {
    //avoid alternate dimension dark when seeing from overworld
    @Redirect(
        method = "Lnet/minecraft/client/renderer/FogRenderer;setupColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 redirectCameraGetPos(Camera camera) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null && AlternateDimensions.isAlternateDimension(world)) {
            return new Vec3(
                camera.getPosition().x,
                Math.max(32.0, camera.getPosition().y),
                camera.getPosition().z
            );
        }
        else {
            return camera.getPosition();
        }
    }
}
