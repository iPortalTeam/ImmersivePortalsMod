package qouteall.imm_ptl.core.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

// temporary work around for bugs
public class DubiousThings {
    public static void init() {
        IPGlobal.postClientTickSignal.connect(DubiousThings::tick);
    }
    
    private static void tick() {
        ClientLevel world = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (world == null) {
            return;
        }
        if (player == null) {
            return;
        }
        if (world.getGameTime() % 233 == 34) {
            checkClientPlayerState();
        }
    }
    
    private static void checkClientPlayerState() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != client.player.level) {
            Helper.err("Player world abnormal");
            //don't know how to fix it
        }
        if (!client.player.isRemoved()) {
            Entity playerInWorld = client.level.getEntity(client.player.getId());
            if (playerInWorld != client.player) {
                Helper.err("Client Player Mismatch");
                if (playerInWorld instanceof LocalPlayer) {
                    client.player = ((LocalPlayer) playerInWorld);
                    Helper.log("Force corrected");
                }
            }
        }
    }
}
