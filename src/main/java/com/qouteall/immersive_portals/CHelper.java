package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.render.PortalRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.dimension.DimensionType;

@Environment(EnvType.CLIENT)
public class CHelper {
    public static PlayerListEntry getClientPlayerListEntry() {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(
            MinecraftClient.getInstance().player.getGameProfile().getId()
        );
    }
    
    public static DimensionType getOriginalDimension() {
        if (CGlobal.renderer.isRendering()) {
            return PortalRenderer.originalPlayerDimension;
        }
        else {
            return MinecraftClient.getInstance().world.dimension.getType();
        }
    }
}
