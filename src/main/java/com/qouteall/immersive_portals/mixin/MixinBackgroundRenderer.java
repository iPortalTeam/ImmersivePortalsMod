package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEBackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer implements IEBackgroundRenderer {
    @Shadow private float red;
    @Shadow private float green;
    @Shadow private float blue;
    
    @Override
    public Vec3d getFogColor() {
        return new Vec3d(red, green, blue);
    }
}
