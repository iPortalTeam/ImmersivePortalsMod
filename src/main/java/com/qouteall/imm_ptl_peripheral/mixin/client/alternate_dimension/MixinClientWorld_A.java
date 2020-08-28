package com.qouteall.imm_ptl_peripheral.mixin.client.alternate_dimension;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientWorld.class)
public class MixinClientWorld_A {
//
//    @Redirect(
//        method = "method_23777",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/biome/Biome;getSkyColor()I"
//        )
//    )
//    private int redirectBiomeGetSkyColor(Biome biome) {
//        ClientWorld this_ = (ClientWorld) ((Object) this);
//        if (this_.getDimension() instanceof AlternateDimension) {
//            return Biomes.PLAINS.getSkyColor();
//        }
//        else {
//            return biome.getSkyColor();
//        }
//    }
}
