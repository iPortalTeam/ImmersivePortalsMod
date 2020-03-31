package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Shaders.class, remap = false)
public class MixinShaders_DimensionRedirect {
    
    @Redirect(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/DimensionType;getRawId()I"
        )
    )
    private static int redirectGetDimensionRawId(DimensionType dimensionType) {
        return RenderDimensionRedirect.getRedirectedDimension(dimensionType).getRawId();
    }
    
    //redirect dimension for shadow camera
    @Redirect(
        method = "Lnet/optifine/shaders/Shaders;setCameraShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;"
        )
    )
    private static ClientWorld redirectWorldForShadowCamera(MinecraftClient client) {
        return CGlobal.clientWorldLoader.getWorld(
            RenderDimensionRedirect.getRedirectedDimension(
                client.world.getDimension().getType()
            )
        );
    }
    
    @Redirect(
        method = "beginRender",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",
            ordinal = 1
        )
    )
    private static ClientWorld redirectWorldInBeginRender(MinecraftClient client) {
        return CGlobal.clientWorldLoader.getWorld(
            RenderDimensionRedirect.getRedirectedDimension(
                client.world.getDimension().getType()
            )
        );
    }
}
