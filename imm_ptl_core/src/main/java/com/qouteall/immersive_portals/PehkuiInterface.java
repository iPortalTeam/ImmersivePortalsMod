package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.TranslatableText;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PehkuiInterface {
    
    public static boolean isPehkuiPresent = false;
    
    private static boolean messageShown = false;
    
    public static Consumer<Portal> onClientPlayerTeleported = PehkuiInterface::onClientPlayerTeleportDefault;
    
    public static BiConsumer<Entity, Portal> onServerEntityTeleported = (e, p) -> {
    
    };
    
    public static BiFunction<Entity, Float, Float> getBaseScale = (e, d) -> 1.0f;
    
    public static BiConsumer<Entity, Float> setBaseScale = (e, s) -> { };
    
    public static BiFunction<Entity, Float, Float> computeThirdPersonScale = (e, d) -> 1.0f;
    
    public static BiFunction<Entity, Float, Float> computeBlockReachScale = (e, d) -> 1.0f;
    
    public static BiFunction<Entity, Float, Float> computeMotionScale = (e, d) -> 1.0f;
    
    private static void onClientPlayerTeleportDefault(Portal portal) {
        showMissingPehkui(portal);
    }
    
    @Environment(EnvType.CLIENT)
    private static void showMissingPehkui(Portal portal) {
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
