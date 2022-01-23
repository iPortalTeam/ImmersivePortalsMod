package qouteall.imm_ptl.core.mixin.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

@Mixin(ParticleEngine.class)
public class MixinParticleManager implements IEParticleManager {
    @Shadow
    protected ClientLevel level;
    
    // skip particle rendering for far portals
    @Inject(
        method = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBeginRenderParticles(
        PoseStack matrixStack, MultiBufferSource.BufferSource immediate,
        LightTexture lightmapTextureManager, Camera camera, float f, CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            if (RenderStates.getRenderedPortalNum() > 4) {
                ci.cancel();
            }
        }
    }
    
    // maybe incompatible with sodium and iris
    @Redirect(
        method = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"
        )
    )
    private void redirectBuildGeometry(Particle instance, VertexConsumer vertexConsumer, Camera camera, float v) {
        if (RenderStates.shouldRenderParticle(instance)) {
            instance.render(vertexConsumer, camera, v);
        }
    }
    
    // a lava ember particle can generate a smoke particle during ticking
    // avoid generating the particle into the wrong dimension
    @Inject(method = "Lnet/minecraft/client/particle/ParticleEngine;tickParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void onTickParticle(Particle particle, CallbackInfo ci) {
        if (((IEParticle) particle).portal_getWorld() != Minecraft.getInstance().level) {
            ci.cancel();
        }
    }
    
    @Override
    public void ip_setWorld(ClientLevel world_) {
        level = world_;
    }
    
}
