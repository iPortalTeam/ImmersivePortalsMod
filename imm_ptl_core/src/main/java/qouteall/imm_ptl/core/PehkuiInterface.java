package qouteall.imm_ptl.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.TranslatableText;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
        
        public float getBaseScale(Entity entity) {
            return getBaseScale(entity, 1.0f);
        }
        
        public float getBaseScale(Entity entity, float tickDelta) {
            return getBaseScale.apply(entity, tickDelta);
        }
        
        public void setBaseScale(Entity entity, float scale) {
            setBaseScale.accept(entity, scale);
        }
        
        public float computeThirdPersonScale(Entity entity, float tickDelta) {
            return computeThirdPersonScale.apply(entity, tickDelta);
        }
        
        public float computeBlockReachScale(Entity entity) {
            return computeBlockReachScale(entity, 1.0f);
        }
        
        public float computeBlockReachScale(Entity entity, float tickDelta) {
            return computeBlockReachScale.apply(entity, tickDelta);
        }
        
        public float computeMotionScale(Entity entity) {
            return computeMotionScale(entity, 1.0f);
        }
        
        public float computeMotionScale(Entity entity, float tickDelta) {
            return computeMotionScale.apply(entity, tickDelta);
        }
    }
    
    public static Invoker invoker = new Invoker();
    
    public static BiFunction<Entity, Float, Float> getBaseScale = (e, d) -> 1.0f;
    
    public static BiConsumer<Entity, Float> setBaseScale = (e, s) -> { };
    
    public static BiFunction<Entity, Float, Float> computeThirdPersonScale = (e, d) -> 1.0f;
    
    public static BiFunction<Entity, Float, Float> computeBlockReachScale = (e, d) -> 1.0f;
    
    public static BiFunction<Entity, Float, Float> computeMotionScale = (e, d) -> 1.0f;
    
    private static boolean messageShown = false;
    
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
