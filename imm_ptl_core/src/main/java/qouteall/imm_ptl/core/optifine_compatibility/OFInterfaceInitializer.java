package qouteall.imm_ptl.core.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.OFInterface;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.optifine.Config;
import net.optifine.shaders.ShaderPackDefault;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Field;

public class OFInterfaceInitializer {
    private static Field gameRenderer_fogStandard;
    
    private static Field worldRenderer_renderInfosNormal;
    
    public static void init() {
        Validate.isTrue(OFInterface.isOptifinePresent);
        
        OFInterface.isShaders = Config::isShaders;
        
        OFInterface.createNewRenderInfosNormal = newWorldRenderer1 -> {
            /**{@link WorldRenderer#chunkInfos}*/
            //in vanilla it will create new chunkInfos object every frame
            //but with optifine it will always use one object
            //we need to switch chunkInfos correctly
            //if we do not put it a new object, it will clear the original chunkInfos
            
            if (worldRenderer_renderInfosNormal == null) {
                worldRenderer_renderInfosNormal = Helper.noError(() ->
                    WorldRenderer.class.getDeclaredField("renderInfosNormal")
                );
                worldRenderer_renderInfosNormal.setAccessible(true);
            }
            
            Helper.noError(() -> {
                worldRenderer_renderInfosNormal.set(newWorldRenderer1, new ObjectArrayList<>(512));
                return null;
            });
        };
        
    }
}
