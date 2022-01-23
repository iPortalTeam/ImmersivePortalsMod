package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

@Mixin(Projectile.class)
public abstract class MixinProjectileEntity extends MixinEntity {
    
    @Shadow
    public abstract void onHit(HitResult hitResult);
    
    @Inject(method = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At(value = "HEAD"), cancellable = true)
    protected void onHit(HitResult hitResult, CallbackInfo ci) {
        if (hitResult instanceof BlockHitResult) {
            Block hittingBlock = this.level.getBlockState(((BlockHitResult) hitResult).getBlockPos()).getBlock();
            if (hitResult.getType() == HitResult.Type.BLOCK &&
                hittingBlock == PortalPlaceholderBlock.instance
            ) {
                ci.cancel();
            }
        }
    }
    
    
}