package com.qouteall.immersive_portals.mixin.common.collision;

import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity_C {
    //fix climbing onto ladder cross portal
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void onGetBlockState(CallbackInfoReturnable<BlockState> cir) {
        Portal collidingPortal = ((IEEntity) this).getCollidingPortal();
        Entity this_ = (Entity) (Object) this;
        if (collidingPortal != null) {
            if (collidingPortal.getNormal().y > 0) {
                BlockPos remoteLandingPos = new BlockPos(
                    collidingPortal.transformPoint(this_.getPos())
                );
                
                BlockState result = CollisionHelper.getWorld(
                    this_.world.isClient,
                    collidingPortal.dimensionTo
                ).getBlockState(remoteLandingPos);
                
                if (!result.isAir()) {
                    cir.setReturnValue(result);
                    cir.cancel();
                }
            }
        }
    }
}
