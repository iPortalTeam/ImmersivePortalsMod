package qouteall.imm_ptl.core.mixin.client.render.optimization;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

// Optimize cloud rendering by storing the context and
// avoiding rebuild the cloud mesh every time
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer_Clouds {
    
    @Shadow
    private int lastCloudsBlockX;
    
    @Shadow
    private int lastCloudsBlockY;
    
    @Shadow
    private int lastCloudsBlockZ;
    
    @Shadow
    @Nullable
    private VertexBuffer cloudsBuffer;
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    private boolean cloudsDirty;
    
    @Shadow
    private int ticks;
    
    @Inject(
        method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FDDD)V",
        at = @At("HEAD")
    )
    private void onBeginRenderClouds(
        MatrixStack matrices, Matrix4f matrix4f,
        float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci
    ) {
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (IPGlobal.cloudOptimization) {
            portal_onBeginCloudRendering(tickDelta, cameraX, cameraY, cameraZ);
        }
    }
    
    @Inject(
        method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FDDD)V",
        at = @At("RETURN")
    )
    private void onEndRenderClouds(MatrixStack matrices, Matrix4f matrix4f, float f, double d, double e, double g, CallbackInfo ci) {
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (IPGlobal.cloudOptimization) {
            portal_onEndCloudRendering();
        }
    }
    
    private void portal_yieldCloudContext(CloudContext context) {
        Vec3d cloudsColor = this.world.getCloudsColor(RenderStates.tickDelta);
        
        context.lastCloudsBlockX = lastCloudsBlockX;
        context.lastCloudsBlockY = lastCloudsBlockY;
        context.lastCloudsBlockZ = lastCloudsBlockZ;
        context.cloudsBuffer = cloudsBuffer;
        context.dimension = world.getRegistryKey();
        context.cloudColor = cloudsColor;
        
        cloudsBuffer = null;
        cloudsDirty = true;
    }
    
    private void portal_loadCloudContext(CloudContext context) {
        Validate.isTrue(context.dimension == world.getRegistryKey());
        
        lastCloudsBlockX = context.lastCloudsBlockX;
        lastCloudsBlockY = context.lastCloudsBlockY;
        lastCloudsBlockZ = context.lastCloudsBlockZ;
        cloudsBuffer = context.cloudsBuffer;
        
        cloudsDirty = false;
    }
    
    private void portal_onBeginCloudRendering(
        float tickDelta, double cameraX, double cameraY, double cameraZ
    ) {
        float f = this.world.getSkyProperties().getCloudsHeight();
        float g = 12.0F;
        float h = 4.0F;
        double d = 2.0E-4D;
        double e = (double) (((float) this.ticks + tickDelta) * 0.03F);
        double i = (cameraX + e) / 12.0D;
        double j = (double) (f - (float) cameraY + 0.33F);
        double k = cameraZ / 12.0D + 0.33000001311302185D;
        i -= (double) (MathHelper.floor(i / 2048.0D) * 2048);
        k -= (double) (MathHelper.floor(k / 2048.0D) * 2048);
        float l = (float) (i - (double) MathHelper.floor(i));
        float m = (float) (j / 4.0D - (double) MathHelper.floor(j / 4.0D)) * 4.0F;
        float n = (float) (k - (double) MathHelper.floor(k));
        Vec3d cloudsColor = this.world.getCloudsColor(tickDelta);
        int kx = (int) Math.floor(i);
        int ky = (int) Math.floor(j / 4.0D);
        int kz = (int) Math.floor(k);
        
        @Nullable CloudContext context = CloudContext.findAndTakeContext(
            kx, ky, kz, world.getRegistryKey(), cloudsColor
        );
        
        if (context != null) {
            portal_loadCloudContext(context);
        }
    }
    
    private void portal_onEndCloudRendering() {
        if (!cloudsDirty) {
            final CloudContext newContext = new CloudContext();
            portal_yieldCloudContext(newContext);
            
            CloudContext.appendContext(newContext);
        }
    }
}
