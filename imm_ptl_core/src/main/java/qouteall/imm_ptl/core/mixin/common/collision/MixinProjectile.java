package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Projectile.class)
public abstract class MixinProjectile extends MixinEntity {
    
    // make it recognize the owner in another dimension
    @Redirect(
        method = "getOwner",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getEntity(Ljava/util/UUID;)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private Entity redirectGetEntityFromUuid(
        net.minecraft.server.level.ServerLevel serverLevel,
        java.util.UUID uuid
    ) {
        MinecraftServer server = serverLevel.getServer();
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
    
//    @Shadow
//    public abstract void onHit(HitResult hitResult);
//
//    @Inject(method = "Lnet/minecraft/world/entity/projectile/Projectile;onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At(value = "HEAD"), cancellable = true)
//    protected void onHit(HitResult hitResult, CallbackInfo ci) {
//        Entity this_ = (Entity) (Object) this;
//        if (hitResult instanceof BlockHitResult) {
//            Block hittingBlock = this_.level().getBlockState(((BlockHitResult) hitResult).getBlockPos()).getBlock();
//            if (hitResult.getType() == HitResult.Type.BLOCK &&
//                hittingBlock == PortalPlaceholderBlock.instance
//            ) {
//                ci.cancel();
//            }
//        }
//    }
//
    
}