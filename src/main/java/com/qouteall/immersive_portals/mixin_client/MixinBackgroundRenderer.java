package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.FogRendererContext;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
    }
}
