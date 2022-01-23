package qouteall.imm_ptl.core.ducks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;

public interface IEGameRenderer {
    void setLightmapTextureManager(LightTexture manager);
    
    boolean getDoRenderHand();
    
    void setCamera(Camera camera);
    
    void setIsRenderingPanorama(boolean cond);
    
    void portal_bobView(PoseStack matrixStack, float tickDelta);
}
