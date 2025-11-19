package qouteall.imm_ptl.core.compat.mixin.iris;

import com.mojang.blaze3d.shaders.Program;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;
import qouteall.q_misc_util.Helper;

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
    
    @Unique
    private boolean immptlPatched = false;
    
    @Inject(
        method = "patchShaders",
        at = @At("RETURN")
    )
    private void onPatchShaderEnds(ChunkVertexType chunkVertexType, CallbackInfo ci) {
        if (!immptlPatched) {
            immptlPatched = true;
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
        else {
            Helper.err("iris terrain shader ImmPtl patched twice");
        }
    }
}
