package qouteall.imm_ptl.peripheral.platform_specific;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

public class IPFeatureControl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final boolean ENABLE_VANILLA_BEHAVIOR_CHANGING_FEATURES;
    
    static {
        ModContainer modContainer = FabricLoader.getInstance()
            .getModContainer("iportal").orElseThrow();
        
        boolean isProvidedByJarInJar = modContainer.getContainingMod().isPresent();
        
        ENABLE_VANILLA_BEHAVIOR_CHANGING_FEATURES = !isProvidedByJarInJar;
//        ENABLE_VANILLA_BEHAVIOR_CHANGING_FEATURES = false;
        
        if (ENABLE_VANILLA_BEHAVIOR_CHANGING_FEATURES) {
            LOGGER.info("Immersive Portals vanilla behavior changing features are enabled");
        }
        else {
            LOGGER.warn("""
                
                Immersive Portals mod is provided by jar-in-jar, so some features are disabled:
                * Nether portal and End portal will be vanilla, regardless of netherPortalMode and endPortalMode in the config.
                * Dimension stack disabled. Dimension stack preset is disabled. Bedrock replacement is disabled.
                Note that these features are still enabled:
                * Datapack-based custom portal generation is still enabled.
                * Portal wand and `/portal` commands are still enabled (except command `/portal dimension_stack`).
                """);
        }
    }
    
    public static boolean isVanillaChangingFeaturesEnabled() {
        return ENABLE_VANILLA_BEHAVIOR_CHANGING_FEATURES;
    }
}
