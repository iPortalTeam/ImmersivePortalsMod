package com.qouteall.immersive_portals.mixin.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface IEParticle {
    @Accessor("world")
    ClientWorld portal_getWorld();
}
