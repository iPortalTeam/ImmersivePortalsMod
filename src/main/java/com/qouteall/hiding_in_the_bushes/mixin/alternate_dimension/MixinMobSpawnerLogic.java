package com.qouteall.hiding_in_the_bushes.mixin.alternate_dimension;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(MobSpawnerLogic.class)
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
