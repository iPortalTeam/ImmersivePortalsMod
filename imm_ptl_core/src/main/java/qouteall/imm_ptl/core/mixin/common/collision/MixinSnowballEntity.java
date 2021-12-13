package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.block.Block;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

@Mixin(SnowballEntity.class)
public abstract class MixinSnowballEntity extends MixinEntity {
    @Shadow
    public abstract void onCollision(HitResult hitResult);
    
    @Inject(method = "onCollision", at = @At(value = "HEAD"), cancellable = true)
    protected void onCollision(HitResult hitResult, CallbackInfo ci) {
        if (hitResult instanceof BlockHitResult) {
            Block hittingBlock = this.world.getBlockState(((BlockHitResult) hitResult).getBlockPos()).getBlock();
            if (hitResult.getType() == HitResult.Type.BLOCK &&
                hittingBlock == PortalPlaceholderBlock.instance
            ) {
                ci.cancel();
            }
        }
    }
}
