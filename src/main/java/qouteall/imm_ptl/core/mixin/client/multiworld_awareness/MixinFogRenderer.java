package qouteall.imm_ptl.core.mixin.client.multiworld_awareness;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.render.context_management.FogRendererContext;

@Mixin(value = FogRenderer.class, priority = 1100)
public class MixinFogRenderer {
    @Shadow
    private static boolean fogEnabled;
    
    static {
        FogRendererContext.copyContextFromObject = context -> {
            fogEnabled = context.fogEnabled;
        };
        
        FogRendererContext.copyContextToObject = context -> {
            context.fogEnabled = fogEnabled;
        };
        
        FogRendererContext.getCurrentFogColor =
            FogRendererContext::getCurrentFogColorValue;
        
        FogRendererContext.init();
    }
}
