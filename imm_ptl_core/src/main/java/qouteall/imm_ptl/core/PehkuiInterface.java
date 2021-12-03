package qouteall.imm_ptl.core;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.TranslatableText;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class PehkuiInterface {
    
    public static class Invoker {
        public boolean isPehkuiPresent() {
            return false;
        }
        
        public void onClientPlayerTeleported(Portal portal) {
            showMissingPehkui(portal);
        }
        
        public void onServerEntityTeleported(Entity entity, Portal portal) {
        
        }
        
        public float getScale(Entity entity) {
            return 1;
        }
        
        public float getMotionScale(Entity entity) {
            return 1;
        }
    }
    
    public static Invoker invoker = new Invoker();
    
    private static boolean messageShown = false;
    
    @Environment(EnvType.CLIENT)
    private static void showMissingPehkui(Portal portal) {
        if (O_O.isForge()) {
            return;
        }
        if (portal.hasScaling() && portal.teleportChangesScale) {
            if (!messageShown) {
                messageShown = true;
                MinecraftClient.getInstance().inGameHud.setOverlayMessage(
                    new TranslatableText("imm_ptl.needs_pehkui"), false
                );
            }
        }
    }
    
}
