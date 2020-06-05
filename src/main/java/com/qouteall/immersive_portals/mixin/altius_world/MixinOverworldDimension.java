package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusGeneratorType;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OverworldDimension.class)
public class MixinOverworldDimension {
//    //Altius overworld is mostly the same as default
//    //TODO support other world gen mods
//    @Redirect(
//        method = "createChunkGenerator",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/level/LevelProperties;getGeneratorType()Lnet/minecraft/world/level/LevelGeneratorType;"
//        )
//    )
//    private LevelGeneratorType redirectGetGeneratorType(LevelProperties levelProperties) {
//        LevelGeneratorType generatorType = levelProperties.getGeneratorType();
//        if (generatorType != AltiusGeneratorType.generatorType) {
//            return generatorType;
//        }
//        else {
//            return LevelGeneratorType.DEFAULT;
//        }
//    }
}
