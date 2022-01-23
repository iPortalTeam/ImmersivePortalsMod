package qouteall.imm_ptl.core.mixin.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface IEParticle {
    @Accessor("level")
    ClientLevel portal_getWorld();
}
