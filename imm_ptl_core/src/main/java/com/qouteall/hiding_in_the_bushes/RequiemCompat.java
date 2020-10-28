package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;

public class RequiemCompat {
    private static boolean isRequiemPresent = false;
    private static Class class_RequiemPlayer;
    private static Class class_PossessionComponent;
    private static Method method_asPossessor;
    private static Method method_getPossessedEntity;
    
    public static boolean getIsRequiemPresent() {
        return isRequiemPresent;
    }
    
    public static void init() {
        isRequiemPresent = FabricLoader.getInstance().isModLoaded("requiem");
        
        if (!isRequiemPresent) {
            return;
        }
        
        class_RequiemPlayer = Helper.noError(() ->
            Class.forName("ladysnake.requiem.api.v1.RequiemPlayer"));
        
        class_PossessionComponent = Helper.noError(() ->
            Class.forName("ladysnake.requiem.api.v1.possession.PossessionComponent"));
        
        method_asPossessor = Helper.noError(() ->
            class_RequiemPlayer.getDeclaredMethod("asPossessor"));
        
        method_getPossessedEntity = Helper.noError(() ->
            class_PossessionComponent.getDeclaredMethod("getPossessedEntity"));
        
        
    }
    
    public static MobEntity getPossessedEntity(PlayerEntity player) {
        Validate.isTrue(isRequiemPresent);
        
        Object possessionComponent = Helper.noError(() -> method_asPossessor.invoke(player));
        Object possessedEntity = Helper.noError(() ->
            method_getPossessedEntity.invoke(possessionComponent));
        return (MobEntity) possessedEntity;
    }
    
    @Environment(EnvType.CLIENT)
    public static void onPlayerTeleportedClient() {
        if (!isRequiemPresent) {
            return;
        }
        
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        MobEntity possessedEntity = getPossessedEntity(player);
        if (possessedEntity != null) {
            if (possessedEntity.world != player.world) {
                Helper.log("Move Requiem Possessed Entity at Client");
                ClientTeleportationManager.moveClientEntityAcrossDimension(
                    possessedEntity,
                    ((ClientWorld) player.world),
                    player.getPos()
                );
            }
        }
    }
    
    public static void onPlayerTeleportedServer(ServerPlayerEntity player) {
        if (!isRequiemPresent) {
            return;
        }
        
        MobEntity possessedEntity = getPossessedEntity(player);
        if (possessedEntity != null) {
            if (possessedEntity.world != player.world) {
                Helper.log("Move Requiem Posessed Entity at Server");
                Global.serverTeleportationManager.changeEntityDimension(
                    possessedEntity,
                    player.world.getRegistryKey(),
                    McHelper.getEyePos(player),
                    false
                );
            }
        }
    }
    
    
}
