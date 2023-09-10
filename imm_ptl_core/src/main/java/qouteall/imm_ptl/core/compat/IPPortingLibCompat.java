package qouteall.imm_ptl.core.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.imm_ptl.core.ducks.IEFrameBuffer;
import qouteall.q_misc_util.Helper;

import java.lang.reflect.Field;

public class IPPortingLibCompat {
    
    public static boolean isPortingLibPresent = false;
    
    private static Field f_port_lib$stencilEnabled;
    
    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("porting_lib")) {
            Helper.log("Porting Lib is present");
            isPortingLibPresent = true;
            
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                f_port_lib$stencilEnabled = Helper.noError(
                    () -> RenderTarget.class.getDeclaredField("port_lib$stencilEnabled")
                );
                f_port_lib$stencilEnabled.setAccessible(true);
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static boolean getIsStencilEnabled(RenderTarget renderTarget) {
        if (isPortingLibPresent) {
            return Helper.noError(
                () -> (Boolean) f_port_lib$stencilEnabled.get(renderTarget)
            );
        }
        else {
            return ((IEFrameBuffer) renderTarget).getIsStencilBufferEnabled();
        }
    }
    
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static void setIsStencilEnabled(RenderTarget renderTarget, boolean cond) {
        if (isPortingLibPresent) {
            
            boolean oldValue = getIsStencilEnabled(renderTarget);
            
            if (oldValue != cond) {
                Helper.noError(
                    () -> {
                        f_port_lib$stencilEnabled.set(renderTarget, cond);
                        return null;
                    }
                );
                renderTarget.resize(
                    renderTarget.viewWidth, renderTarget.viewHeight, Minecraft.ON_OSX
                );
            }
        }
        else {
            ((IEFrameBuffer) renderTarget).setIsStencilBufferEnabledAndReload(cond);
        }
        
    }
}
