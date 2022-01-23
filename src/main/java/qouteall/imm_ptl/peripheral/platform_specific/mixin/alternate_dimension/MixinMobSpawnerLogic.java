package qouteall.imm_ptl.peripheral.platform_specific.mixin.alternate_dimension;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseSpawner.class)
public class MixinMobSpawnerLogic {
//    //spawn regardless of light
//    @Redirect(
//        method = "update",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/mob/MobEntity;canSpawn(Lnet/minecraft/world/WorldView;)Z"
//        )
//    )
//    private boolean redirectCanSpawn1(MobEntity mobEntity, WorldView world) {
//        if (mobEntity.world.getDimension() instanceof AlternateDimension) {
//            return true;
//        }
//        return mobEntity.canSpawn(world);
//    }
//
//    //spawn regardless of light
//    @Redirect(
//        method = "update",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/SpawnRestriction;canSpawn(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/IWorld;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)Z"
//        )
//    )
//    private boolean redirectCanSpawn2(
//        EntityType type,
//        WorldAccess world,
//        SpawnReason spawnReason,
//        BlockPos pos,
//        Random random
//    ) {
//        if (world.getDimension() instanceof AlternateDimension) {
//            return true;
//        }
//        return SpawnRestriction.canSpawn(type, world, spawnReason, pos, random);
//    }
}
