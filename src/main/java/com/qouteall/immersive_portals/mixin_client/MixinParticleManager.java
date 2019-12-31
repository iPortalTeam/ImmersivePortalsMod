package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEParticleManager;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ParticleManager.class)
public class MixinParticleManager implements IEParticleManager {
    @Shadow
    protected World world;
    
    @Override
    public void mySetWorld(World world_) {
        world = world_;
    }
}
