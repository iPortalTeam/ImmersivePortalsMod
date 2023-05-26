package qouteall.imm_ptl.core.compat;

import me.drex.vanish.api.VanishAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;

public class IPVanishCompat {
    
    public static boolean isVanishPresent = false;
    
    public static void init(){
        if (FabricLoader.getInstance().isModLoaded("melius-vanish")) {
            isVanishPresent = true;
            Helper.log("Vanish is present");
        }
    }

    public static boolean canSeePlayer(@Nullable Player executive, ServerPlayer viewer) {
        if (isVanishPresent && executive instanceof ServerPlayer executiveServer) {
            return VanishAPI.canSeePlayer(executiveServer, viewer);
        }
        return true;
    }
    
}
