package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.render.FogRendererContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Shadow
    private static float red;
    @Shadow
    private static float green;
    @Shadow
    private static float blue;
    @Shadow
    private static int waterFogColor = -1;
    @Shadow
    private static int nextWaterFogColor = -1;
    @Shadow
    private static long lastWaterFogColorUpdateTime = -1L;
    
    //avoid alternate dimension dark when seeing from overworld
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;getPos()Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private static Vec3d redirectCameraGetPos(Camera camera) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && world.dimension.getType() == ModMain.alternate) {
            return new Vec3d(
                camera.getPos().x,
                Math.max(32.0, camera.getPos().y),
                camera.getPos().z
            );
        }
        else {
            return camera.getPos();
        }
    }
    
    static {
        FogRendererContext.copyContextFromObject = context -> {
            red = context.red;
            green = context.green;
            blue = context.blue;
            waterFogColor = context.waterFogColor;
            nextWaterFogColor = context.nextWaterFogColor;
            lastWaterFogColorUpdateTime = context.lastWaterFogColorUpdateTime;
        };
        
        FogRendererContext.copyContextToObject = context -> {
            context.red = red;
            context.green = green;
            context.blue = blue;
            context.waterFogColor = waterFogColor;
            context.nextWaterFogColor = nextWaterFogColor;
            context.lastWaterFogColorUpdateTime = lastWaterFogColorUpdateTime;
        };
        
        FogRendererContext.getCurrentFogColor =
            () -> new Vec3d(red, green, blue);
        
        FogRendererContext.init();
    }
}
