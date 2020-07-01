package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
    
    // avoid nether fog color being interfered by nether's weather
    // nether should not be raining. maybe another bug
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getRainGradient(F)F"
        )
    )
    private static float redirectGetRainGradient(ClientWorld world, float delta) {
        if (world.getRegistryKey() == World.NETHER) {
            return 0.0f;
        }
        return world.getRainGradient(delta);
    }
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getThunderGradient(F)F"
        )
    )
    private static float redirectGetThunderGradient(ClientWorld world, float delta) {
        if (world.getRegistryKey() == World.NETHER) {
            return 0.0f;
        }
        return world.getThunderGradient(delta);
    }
}
