package qouteall.imm_ptl.core.platform_specific.sodium_compatibility;

import qouteall.imm_ptl.core.SodiumInterface;
import qouteall.imm_ptl.core.render.FrontClipping;
import me.jellysquid.mods.sodium.client.SodiumHooks;
import me.jellysquid.mods.sodium.client.WorldRendererAccessor;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SodiumInterfaceInitializer {
    public static void init() {
        SodiumInterface.createClientChunkManager = ClientChunkManagerWithSodium::new;
        
        SodiumInterface.createNewRenderingContext = worldRenderer -> {
            SodiumWorldRenderer swr = ((WorldRendererAccessor) worldRenderer).getSodiumWorldRenderer();
            return swr.createNewRenderContext();
        };
        
        SodiumInterface.switchRenderingContext = (worldRenderer, newContext) -> {
            SodiumWorldRenderer sodiumWorldRenderer =
                ((WorldRendererAccessor) worldRenderer).getSodiumWorldRenderer();
            
            // must update render list
            sodiumWorldRenderer.scheduleTerrainUpdate();
            
            return sodiumWorldRenderer.switchRenderContext(((ChunkRenderManager.RenderContext) newContext));
        };
        
        SodiumHooks.shouldEnableClipping = () -> FrontClipping.isClippingEnabled;
        SodiumHooks.getClippingEquation = () -> {
            double[] doubles = FrontClipping.getActiveClipPlaneEquation();
            float[] floats = new float[]{
                (float) doubles[0], (float) doubles[1], (float) doubles[2], (float) doubles[3]
            };
            return floats;
        };
        SodiumHooks.useClipping = () -> true;
    }
}
