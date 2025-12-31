package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Matrix4f;

public interface IEGameRenderer {
    void ip_setLightmapTextureManager(LightTexture manager);
    
    boolean ip_getDoRenderHand();

    void ip_setDoRenderHand(boolean doRenderHand);

    void ip_setCamera(Camera camera);

    void ip_setIsRenderingPanorama(boolean cond);

    void ip_resetProjectionMatrix(Matrix4f matrix);

    FogRenderer ip_getFogRenderer();
}
