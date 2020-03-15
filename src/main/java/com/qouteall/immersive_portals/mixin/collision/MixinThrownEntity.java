package com.qouteall.immersive_portals.mixin.collision;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.thrown.ThrownEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEntity.class)
public abstract class MixinThrownEntity extends MixinProjectile {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/thrown/ThrownEntity;onCollision(Lnet/minecraft/util/hit/HitResult;)V"
            )
    )
    private void redirectCollision(ThrownEntity entity, HitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK || entity.world.getBlockState(((BlockHitResult) hitResult).getBlockPos()).getBlock() != PortalPlaceholderBlock.instance) {
            this.onCollision(hitResult);
        }
    }
}
