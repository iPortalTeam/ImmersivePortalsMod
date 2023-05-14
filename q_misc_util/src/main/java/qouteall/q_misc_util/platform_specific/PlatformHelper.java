package qouteall.q_misc_util.platform_specific;

import net.fabricmc.loader.api.FabricLoader;

public final class PlatformHelper {

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static EnvType getEnvironmentType() {
        switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> {return EnvType.CLIENT;}
            case SERVER -> {return EnvType.SERVER;}
            default -> {return EnvType.UNIMPLEMENTED;}
        }
    }

    private PlatformHelper() {
        throw new UnsupportedOperationException("This is a static only class.");
    }
}
