package qouteall.imm_ptl.core;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import qouteall.imm_ptl.core.platform_specific.sodium_compatibility.ClientChunkManagerWithSodium;

@Environment(EnvType.CLIENT)
public class SodiumInterface {
    
    public static class Invoker {
        public boolean isSodiumPresent() {
            return false;
        }
        
        public ClientChunkManager createClientChunkManager(
            ClientWorld world, int loadDistance
        ) {
            return new MyClientChunkManager(world, loadDistance);
        }
        
        public Object createNewContext() {
            return null;
        }
        
        public void switchContextWithCurrentWorldRenderer(Object context) {
        
        }
    }
    
    public static Invoker invoker = new Invoker();
    
    public static class OnSodiumPresent extends Invoker {
        @Override
        public boolean isSodiumPresent() {
            return true;
        }
        
        @Override
        public ClientChunkManager createClientChunkManager(ClientWorld world, int loadDistance) {
            return new ClientChunkManagerWithSodium(world, loadDistance);
        }
        
        @Override
        public RenderSectionManager.RenderingContext createNewContext() {
            return new RenderSectionManager.RenderingContext();
        }
        
        @Override
        public void switchContextWithCurrentWorldRenderer(Object context) {
            SodiumWorldRenderer swr = SodiumWorldRenderer.getInstance();
            swr.scheduleTerrainUpdate();
            swr.swapRenderingContext(((RenderSectionManager.RenderingContext) context));
            swr.scheduleTerrainUpdate();
        }
    }
    
}
