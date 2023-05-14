package qouteall.imm_ptl.core.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.platform_specific.PlatformHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public class IPFlywheelCompat {
    
    public static boolean isFlywheelPresent = false;
    
    public static void init(){
        if (PlatformHelper.isModLoaded("flywheel")) {
            Helper.log("Flywheel is present");
        }
        
    }
    
}
