package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IERayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RayTraceContext.class)
public abstract class MixinRayTraceContext implements IERayTraceContext {
    @SuppressWarnings("ShadowModifiers")
    @Shadow
    private Vec3d start;

    @SuppressWarnings("ShadowModifiers")
    @Shadow
    private Vec3d end;

    @Override
    public IERayTraceContext setStart(Vec3d newStart) {
        start = newStart;
        return this;
    }

    @Override
    public IERayTraceContext setEnd(Vec3d newEnd) {
        end = newEnd;
        return this;
    }
}
