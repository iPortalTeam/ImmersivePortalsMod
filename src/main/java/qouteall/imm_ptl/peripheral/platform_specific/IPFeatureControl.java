package qouteall.imm_ptl.peripheral.platform_specific;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

public class IPFeatureControl {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static boolean isProvidedByJarInJar() {
        ModContainer modContainer = FabricLoader.getInstance()
            .getModContainer("iportal")
            .orElseThrow(() -> new RuntimeException("iportal mod not found"));
        
        return modContainer.getContainingMod().isPresent();
    }
    
    public static boolean enableVanillaBehaviorChangingByDefault() {
        return !isProvidedByJarInJar();
    }
}
