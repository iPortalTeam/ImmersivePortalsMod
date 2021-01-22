package com.qouteall.immersive_portals.mixin.client.particle;

import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.portal.PortalLike;
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
import net.minecraft.util.math.Vec3d;
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
    
    @Redirect(
        method = "renderParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"
        ),
        require = 0
    )
    private void redirectBuildGeometry(Particle particle, VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (((IEParticle) particle).portal_getWorld() == MinecraftClient.getInstance().world) {
            if (portal_shouldRenderParticle(particle)) {
                particle.buildGeometry(vertexConsumer, camera, tickDelta);
            }
        }
    }
    
    // a lava ember particle can generate a smoke particle during ticking
    // avoid generating the particle into the wrong dimension
    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void onTickParticle(Particle particle, CallbackInfo ci) {
        if (((IEParticle) particle).portal_getWorld() != MinecraftClient.getInstance().world) {
            ci.cancel();
        }
    }
    
    @Override
    public void mySetWorld(ClientWorld world_) {
        world = world_;
    }
    
    private static boolean portal_shouldRenderParticle(Particle particle) {
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            Vec3d particlePos = particle.getBoundingBox().getCenter();
            return renderingPortal.isInside(particlePos, 0.5);
        }
        return true;
    }
}
