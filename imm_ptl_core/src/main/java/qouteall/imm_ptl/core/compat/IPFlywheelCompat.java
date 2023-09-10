package qouteall.imm_ptl.core.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.q_misc_util.Helper;

@Environment(EnvType.CLIENT)
@OnlyIn(Dist.CLIENT)
public class IPFlywheelCompat {
    
    public static boolean isFlywheelPresent = false;
    
    public static void init(){
        if (FabricLoader.getInstance().isModLoaded("flywheel")) {
            Helper.log("Flywheel is present");
        }
        
    }
    
}
