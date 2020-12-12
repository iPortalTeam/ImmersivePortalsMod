package com.qouteall.immersive_portals.mixin.client.particle;

import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class MixinParticleManager implements IEParticleManager {
    @Shadow
    protected ClientWorld world;
    
    // skip particle rendering for far portals
    @Inject(
        method = "renderParticles",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBeginRenderParticles(
        MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate,
        LightmapTextureManager lightmapTextureManager, Camera camera, float f, CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            if (RenderStates.getRenderedPortalNum() > 4) {
                ci.cancel();
            }
        }
    }
    
    // if the particle does not belong to the current dimension, do not render
    @Redirect(
        method = "renderParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"
        )
    )
    private void redirectBuildGeometry(Particle particle, VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (((IEParticle) particle).portal_getWorld() == MinecraftClient.getInstance().world) {
            particle.buildGeometry(vertexConsumer, camera, tickDelta);
        }
    }
    
    @Override
    public void mySetWorld(ClientWorld world_) {
        world = world_;
    }
}