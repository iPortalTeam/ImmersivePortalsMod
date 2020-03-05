package com.qouteall.hiding_in_the_bushes.mixin.alternate_dimension;

import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import net.minecraft.entity.SpawnType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.IWorld;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobSpawnerLogic.class)
public class MixinMobSpawnerLogic {
    //spawn regardless of light
    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/mob/MobEntity;canSpawn(Lnet/minecraft/world/WorldView;)Z"
        )
    )
    private boolean redirectCanSpawn1(MobEntity mobEntity, WorldView world) {
        if (mobEntity.world.dimension instanceof AlternateDimension) {
            return true;
        }
        return mobEntity.canSpawn(world);
    }
    
    //spawn regardless of light
    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/mob/MobEntity;canSpawn(Lnet/minecraft/world/IWorld;Lnet/minecraft/entity/SpawnType;)Z"
        )
    )
    private boolean redirectCanSpawn2(MobEntity mobEntity, IWorld world, SpawnType spawnType) {
        if (mobEntity.world.dimension instanceof AlternateDimension) {
            return true;
        }
        return mobEntity.canSpawn(world);
    }
}
