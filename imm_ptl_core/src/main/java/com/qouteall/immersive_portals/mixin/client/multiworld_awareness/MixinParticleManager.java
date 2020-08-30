package com.qouteall.immersive_portals.mixin.client.multiworld_awareness;

import com.qouteall.immersive_portals.ducks.IEParticleManager;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ParticleManager.class)
public class MixinParticleManager implements IEParticleManager {
    @Shadow
    protected ClientWorld world;
    
//    //currently particle manager cannot handle particles in different dimensions
//    @Inject(
//        method = "Lnet/minecraft/client/particle/ParticleManager;addParticle(Lnet/minecraft/client/particle/Particle;)V",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onAddParticle(Particle particle, CallbackInfo ci) {
//        if (CGlobal.clientWorldLoader.isClientRemoteTicking) {
//            ci.cancel();
//        }
//    }
    
    @Override
    public void mySetWorld(ClientWorld world_) {
        world = world_;
    }
}
