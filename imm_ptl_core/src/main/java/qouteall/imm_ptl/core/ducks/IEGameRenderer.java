package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;

public interface IEGameRenderer {
    void setLightmapTextureManager(LightmapTextureManager manager);
    
    boolean getDoRenderHand();
    
    void setDoRenderHand(boolean e);
    
    void setCamera(Camera camera);
    
    void setIsRenderingPanorama(boolean cond);
}
