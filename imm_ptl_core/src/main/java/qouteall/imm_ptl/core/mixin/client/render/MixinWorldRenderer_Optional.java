package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

// avoid crashing with sodium
// the overwrite has priority of 1000
@Mixin(value = WorldRenderer.class, priority = 1100)
public class MixinWorldRenderer_Optional {
    @Shadow
    private BuiltChunkStorage chunks;
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    //avoid translucent sort while rendering portal
    @Redirect(
        method = "renderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getTranslucent()Lnet/minecraft/client/render/RenderLayer;",
            ordinal = 0
        ),
        require = 0
    )
    private RenderLayer redirectGetTranslucent() {
        if (PortalRendering.isRendering()) {
            return null;
        }
        return RenderLayer.getTranslucent();
    }
    
    //update builtChunkStorage every frame
    //update terrain when rendering portal
    @Inject(
        method = "setupTerrain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/chunk/ChunkBuilder;setCameraPosition(Lnet/minecraft/util/math/Vec3d;)V"
        ),
        require = 0
    )
    private void onBeforeChunkBuilderSetCameraPosition(
        Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci
    ) {
        if (IPCGlobal.useHackedChunkRenderDispatcher) {
            this.chunks.updateCameraPosition(this.client.player.getX(), this.client.player.getZ());
        }
        
//        if (PortalRendering.isRendering()) {
//            field_34810 = true;
//        }
    }
    
//    //rebuild less chunk in render thread while rendering portal to reduce lag spike
//    //minecraft has two places rebuilding chunks in render thread
//    //one in updateChunks() one in setupTerrain()
//    @ModifyConstant(
//        method = "setupTerrain",
//        constant = @Constant(doubleValue = 768.0D),
//        require = 0
//    )
//    private double modifyRebuildRange(double original) {
//        if (PortalRendering.isRendering()) {
//            return 256.0;
//        }
//        else {
//            return original;
//        }
//    }
    
    //the camera position is used for translucent sort
    //avoid messing it
    @Redirect(
        method = "setupTerrain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/chunk/ChunkBuilder;setCameraPosition(Lnet/minecraft/util/math/Vec3d;)V"
        ),
        require = 0
    )
    private void onSetChunkBuilderCameraPosition(ChunkBuilder chunkBuilder, Vec3d cameraPosition) {
        if (PortalRendering.isRendering()) {
            if (client.world.getRegistryKey() == RenderStates.originalPlayerDimension) {
                return;
            }
        }
        chunkBuilder.setCameraPosition(cameraPosition);
    }
    
    @Inject(
        method = "renderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/BufferRenderer;unbindAll()V"
        ),
        require = 0
    )
    private void onGetShaderInRenderingLayer(
        RenderLayer renderLayer, MatrixStack matrices,
        double x, double y, double z, Matrix4f matrix4f, CallbackInfo ci
    ) {
        FrontClipping.updateClippingEquationUniformForCurrentShader(false);
    }
}
