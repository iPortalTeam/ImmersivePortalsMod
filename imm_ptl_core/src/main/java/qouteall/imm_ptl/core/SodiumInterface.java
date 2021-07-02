package qouteall.imm_ptl.core;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import qouteall.imm_ptl.core.platform_specific.sodium_compatibility.ClientChunkManagerWithSodium;

import java.util.function.BiFunction;
import java.util.function.Function;

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
        
        public RenderSectionManager.RenderingContext createNewContext() {
            return null;
        }
        
        public void switchContextWithCurrentWorldRenderer(RenderSectionManager.RenderingContext context) {
        
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
        public void switchContextWithCurrentWorldRenderer(RenderSectionManager.RenderingContext context) {
            SodiumWorldRenderer swr = SodiumWorldRenderer.getInstance();
            swr.scheduleTerrainUpdate();
            swr.swapRenderingContext(context);
            swr.scheduleTerrainUpdate();
        }
    }
    
}
