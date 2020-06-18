package com.qouteall.immersive_portals.mixin_client.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
