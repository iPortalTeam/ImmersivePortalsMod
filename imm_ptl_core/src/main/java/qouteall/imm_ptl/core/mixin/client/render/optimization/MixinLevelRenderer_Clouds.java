package qouteall.imm_ptl.core.mixin.client.render.optimization;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import javax.annotation.Nullable;

// Optimize cloud rendering by storing the context and
// avoiding rebuild the cloud mesh every time
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer_Clouds {
    
    @Shadow
    private int prevCloudX;
    
    @Shadow
    private int prevCloudY;
    
    @Shadow
    private int prevCloudZ;
    
    @Shadow
    @Nullable
    private VertexBuffer cloudBuffer;
    
    @Shadow
    private ClientLevel level;
    
    @Shadow
    private boolean generateClouds;
    
    @Shadow
    private int ticks;
    
    @Inject(
        method = "renderClouds",
        at = @At("HEAD")
    )
    private void onBeginRenderClouds(
        PoseStack matrices, Matrix4f matrix4f,
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
        method = "renderClouds",
        at = @At("RETURN")
    )
    private void onEndRenderClouds(PoseStack matrices, Matrix4f matrix4f, float f, double d, double e, double g, CallbackInfo ci) {
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (IPGlobal.cloudOptimization) {
            portal_onEndCloudRendering();
        }
    }
    
    private void portal_yieldCloudContext(CloudContext context) {
        Vec3 cloudsColor = this.level.getCloudColor(RenderStates.tickDelta);
        
        context.lastCloudsBlockX = prevCloudX;
        context.lastCloudsBlockY = prevCloudY;
        context.lastCloudsBlockZ = prevCloudZ;
        context.cloudsBuffer = cloudBuffer;
        context.dimension = level.dimension();
        context.cloudColor = cloudsColor;
        
        cloudBuffer = null;
        generateClouds = true;
    }
    
    private void portal_loadCloudContext(CloudContext context) {
        Validate.isTrue(context.dimension == level.dimension());
        
        prevCloudX = context.lastCloudsBlockX;
        prevCloudY = context.lastCloudsBlockY;
        prevCloudZ = context.lastCloudsBlockZ;
        cloudBuffer = context.cloudsBuffer;
        
        generateClouds = false;
    }
    
    private void portal_onBeginCloudRendering(
        float tickDelta, double cameraX, double cameraY, double cameraZ
    ) {
        float f = this.level.effects().getCloudHeight();
        float g = 12.0F;
        float h = 4.0F;
        double d = 2.0E-4D;
        double e = (double) (((float) this.ticks + tickDelta) * 0.03F);
        double i = (cameraX + e) / 12.0D;
        double j = (double) (f - (float) cameraY + 0.33F);
        double k = cameraZ / 12.0D + 0.33000001311302185D;
        i -= (double) (Mth.floor(i / 2048.0D) * 2048);
        k -= (double) (Mth.floor(k / 2048.0D) * 2048);
        float l = (float) (i - (double) Mth.floor(i));
        float m = (float) (j / 4.0D - (double) Mth.floor(j / 4.0D)) * 4.0F;
        float n = (float) (k - (double) Mth.floor(k));
        Vec3 cloudsColor = this.level.getCloudColor(tickDelta);
        int kx = (int) Math.floor(i);
        int ky = (int) Math.floor(j / 4.0D);
        int kz = (int) Math.floor(k);
        
        @Nullable CloudContext context = CloudContext.findAndTakeContext(
            kx, ky, kz, level.dimension(), cloudsColor
        );
        
        if (context != null) {
            portal_loadCloudContext(context);
        }
    }
    
    private void portal_onEndCloudRendering() {
        if (!generateClouds) {
            final CloudContext newContext = new CloudContext();
            portal_yieldCloudContext(newContext);
            
            CloudContext.appendContext(newContext);
        }
    }
}
