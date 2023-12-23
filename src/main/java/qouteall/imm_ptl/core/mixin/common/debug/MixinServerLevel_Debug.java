package qouteall.imm_ptl.core.mixin.common.debug;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class MixinServerLevel_Debug {
    @Shadow
    @Final
    private static Logger LOGGER;
    
    // check for it to not add entity to wrong world
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void onAddEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel this_ = (ServerLevel) (Object) this;
        
        if (entity.level() != this_) {
            LOGGER.error("Adding an entity to the wrong dimension {} {}", this, entity);
            cir.setReturnValue(false);
        }
    }
}
