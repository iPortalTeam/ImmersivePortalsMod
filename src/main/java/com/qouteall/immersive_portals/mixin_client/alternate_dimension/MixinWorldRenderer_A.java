package com.qouteall.immersive_portals.mixin_client.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_A {
    //avoid alternate dimension dark sky in low y
    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld$Properties;getSkyDarknessHeight()D"
        )
    )
    private double redirectGetSkyDarknessHeight(ClientWorld.Properties properties) {
        if (ModMain.isAlternateDimension(MinecraftClient.getInstance().world)) {
            return -10000;
        }
        return properties.getSkyDarknessHeight();
    }
}
