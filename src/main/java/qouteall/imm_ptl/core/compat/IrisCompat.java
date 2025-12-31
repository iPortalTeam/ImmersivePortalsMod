package qouteall.imm_ptl.core.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;

public final class IrisCompat {
    private IrisCompat() {
    }

    public static boolean isIrisPresent() {
        return FabricLoader.getInstance().isModLoaded("iris");
    }

    public static boolean isShaders() {
        return false;
    }

    public static boolean isRenderingShadowMap() {
        return false;
    }

    @Nullable
    public static String getShaderpackName() {
        return null;
    }

    public static Object getPipeline(LevelRenderer worldRenderer) {
        return null;
    }

    public static void setPipeline(LevelRenderer worldRenderer, Object pipeline) {
    }

    public static void reloadPipelines() {
    }
}
