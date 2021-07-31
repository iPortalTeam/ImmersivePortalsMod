package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;

public interface IEGameRenderer {
    void setLightmapTextureManager(LightmapTextureManager manager);
    
    boolean getDoRenderHand();
    
    void setCamera(Camera camera);
    
    void setIsRenderingPanorama(boolean cond);
    
    void portal_bobView(MatrixStack matrixStack, float tickDelta);
}
