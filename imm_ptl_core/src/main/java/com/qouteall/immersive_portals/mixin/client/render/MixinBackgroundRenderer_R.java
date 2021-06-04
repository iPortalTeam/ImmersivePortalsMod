package com.qouteall.immersive_portals.mixin.client.render;

import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BackgroundRenderer.class, priority = 900)
public class MixinBackgroundRenderer_R {

    
}
