package qouteall.imm_ptl.core.compat.mixin;

import com.mojang.blaze3d.shaders.Program;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexType;
import net.coderbot.iris.pipeline.SodiumTerrainPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.util.Optional;

@Pseudo
@Mixin(value = SodiumTerrainPipeline.class, remap = false)
public class MixinIrisSodiumSodiumTerrainPipeline {
    @Shadow
    Optional<String> terrainSolidVertex;
    
    @Shadow
    Optional<String> terrainCutoutVertex;
    
    @Shadow
    Optional<String> translucentVertex;
    
    @Inject(
        method = "patchShaders",
        at = @At("RETURN")
    )
    private void onPatchShaderEnds(ChunkVertexType par1, CallbackInfo ci) {
        terrainSolidVertex = terrainSolidVertex.map(code ->
            ShaderCodeTransformation.transform(
                Program.Type.VERTEX,
                "iris_sodium_terrain_vertex",
                code
            )
        );
        terrainCutoutVertex = terrainCutoutVertex.map(code ->
            ShaderCodeTransformation.transform(
                Program.Type.VERTEX,
                "iris_sodium_terrain_vertex",
                code
            )
        );
        translucentVertex = translucentVertex.map(code ->
            ShaderCodeTransformation.transform(
                Program.Type.VERTEX,
                "iris_sodium_terrain_vertex",
                code
            )
        );
    }
}
