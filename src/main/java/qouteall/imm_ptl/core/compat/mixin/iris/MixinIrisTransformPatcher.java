package qouteall.imm_ptl.core.compat.mixin.iris;

import com.mojang.blaze3d.shaders.ShaderType;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

import java.util.HashMap;
import java.util.Map;

@Pseudo
@Mixin(value = TransformPatcher.class, remap = false)
public class MixinIrisTransformPatcher {
    
    @Inject(
        method = "transformInternal",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onTransformInternal(
        String name, Map<PatchShaderType, String> inputs,
        Parameters parameters, CallbackInfoReturnable<Map<PatchShaderType, String>> cir
    ) {
        Map<PatchShaderType, String> map = cir.getReturnValue();
        String code = map.get(PatchShaderType.VERTEX);
        if (code != null) {
            String transformed = ShaderCodeTransformation.transform(ShaderType.VERTEX, "iris_" + name, code);
            map.put(PatchShaderType.VERTEX, transformed);
        }
    }
    
//    @Shadow
//    Optional<String> terrainSolidVertex;
//
//    @Shadow
//    Optional<String> terrainCutoutVertex;
//
//    @Shadow
//    Optional<String> translucentVertex;
//
//    @Unique
//    private boolean immptlPatched = false;
//
//    @Inject(
//        method = "patchShaders",
//        at = @At("RETURN")
//    )
//    private void onPatchShaderEnds(ChunkVertexType chunkVertexType, CallbackInfo ci) {
//        if (!immptlPatched) {
//            immptlPatched = true;
//            terrainSolidVertex = terrainSolidVertex.map(code ->
//                ShaderCodeTransformation.transform(
//                    Program.Type.VERTEX,
//                    "iris_sodium_terrain_vertex",
//                    code
//                )
//            );
//            terrainCutoutVertex = terrainCutoutVertex.map(code ->
//                ShaderCodeTransformation.transform(
//                    Program.Type.VERTEX,
//                    "iris_sodium_terrain_vertex",
//                    code
//                )
//            );
//            translucentVertex = translucentVertex.map(code ->
//                ShaderCodeTransformation.transform(
//                    Program.Type.VERTEX,
//                    "iris_sodium_terrain_vertex",
//                    code
//                )
//            );
//        }
//        else {
//            Helper.err("iris terrain shader ImmPtl patched twice");
//        }
//    }
}
