package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.render.Camera;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Camera.class)
public class MixinCamera {
    /**
     * @author qouteall
     */
    @Overwrite
    public FluidState getSubmergedFluidState() {
        return Fluids.EMPTY.getDefaultState();
    }
}
