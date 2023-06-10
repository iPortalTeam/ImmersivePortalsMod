package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMob {
    @Shadow
    private @Nullable Entity leashHolder;
    
    @Shadow
    public abstract void dropLeash(boolean sendPacket, boolean dropLead);
    
    // drop leash when the player goes to another dimension
    @Inject(
        method = "tickLeash",
        at = @At("RETURN")
    )
    private void onTickLeash(CallbackInfo ci) {
        Mob this_ = (Mob) (Object) this;
        
        if (leashHolder != null) {
            if (leashHolder.level() != this_.level()) {
                dropLeash(true, true);
            }
        }
    }
}
