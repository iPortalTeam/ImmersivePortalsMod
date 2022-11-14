package qouteall.imm_ptl.core.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

public class DubiousThings {
    public static void init() {
        IPGlobal.postClientTickSignal.connect(DubiousThings::tick);
    }
    
    private static void tick() {
        // things removed
    }
    
}
