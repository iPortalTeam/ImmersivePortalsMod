package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.q_misc_util.Helper;

public class DimensionRenderHelper {
    private static final Minecraft client = Minecraft.getInstance();
    public final Level world;
    
    public final LightTexture lightmapTexture;
    
    public DimensionRenderHelper(Level world) {
        this.world = world;
        
        if (client.level == world) {
            IEGameRenderer gameRenderer = (IEGameRenderer) client.gameRenderer;
            
            lightmapTexture = client.gameRenderer.lightTexture();
        }
        else {
            lightmapTexture = new LightTexture(client.gameRenderer, client);
            Helper.log("Created lightmap texture for " + world.dimension().location());
        }
    }
    
    public void tick() {
        if (lightmapTexture != client.gameRenderer.lightTexture()) {
            lightmapTexture.tick();
        }
    }
    
    public void cleanUp() {
        if (lightmapTexture != client.gameRenderer.lightTexture()) {
            lightmapTexture.close();
        }
    }
    
}
