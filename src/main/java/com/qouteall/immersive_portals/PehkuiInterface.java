package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.TranslatableText;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class PehkuiInterface {
    
    public static boolean isPehkuiPresent = false;
    
    private static boolean messageShown = false;
    
    public static Consumer<Portal> onClientPlayerTeleported = PehkuiInterface::onClientPlayerTeleportDefault;
    
    public static BiConsumer<Entity, Portal> onServerEntityTeleported = (e, p) -> {
    
    };
    
    public static Function<Entity, Float> getScale = e -> 1.0f;
    
    private static void onClientPlayerTeleportDefault(Portal portal) {
        showMissingPehkui(portal);
    }
    
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
