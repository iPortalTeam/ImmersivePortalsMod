package qouteall.imm_ptl.core.mixin.client.multiworld_awareness;

import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.render.context_management.FogRendererContext;

@Mixin(value = FogRenderer.class, priority = 1100)
public class MixinBackgroundRenderer {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;
    @Shadow
    private static int targetBiomeFog = -1;
    @Shadow
    private static int previousBiomeFog = -1;
    @Shadow
    private static long biomeChangedTime = -1L;
    
    static {
        FogRendererContext.copyContextFromObject = context -> {
            fogRed = context.red;
            fogGreen = context.green;
            fogBlue = context.blue;
            targetBiomeFog = context.waterFogColor;
            previousBiomeFog = context.nextWaterFogColor;
            biomeChangedTime = context.lastWaterFogColorUpdateTime;
        };
        
        FogRendererContext.copyContextToObject = context -> {
            context.red = fogRed;
            context.green = fogGreen;
            context.blue = fogBlue;
            context.waterFogColor = targetBiomeFog;
            context.nextWaterFogColor = previousBiomeFog;
            context.lastWaterFogColorUpdateTime = biomeChangedTime;
        };
        
        FogRendererContext.getCurrentFogColor =
            () -> new Vec3(fogRed, fogGreen, fogBlue);
        
        FogRendererContext.init();
    }
}
