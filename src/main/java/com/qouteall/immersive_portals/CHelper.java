package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.render.RenderHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.Config;

@Environment(EnvType.CLIENT)
public class CHelper {
    public static PlayerListEntry getClientPlayerListEntry() {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(
            MinecraftClient.getInstance().player.getGameProfile().getId()
        );
    }
    
    //NOTE this may not be reliable
    public static DimensionType getOriginalDimension() {
        if (CGlobal.renderer.isRendering()) {
            return RenderHelper.originalPlayerDimension;
        }
        else {
            return MinecraftClient.getInstance().player.dimension;
        }
    }
    
    public static boolean shouldDisableFog() {
        if (CGlobal.isOptifinePresent) {
            return Config.isFogOff() && MinecraftClient.getInstance().gameRenderer.fogStandard;
        }
        else {
            return false;
        }
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(DimensionType dimension) {
        return CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
    }
}
