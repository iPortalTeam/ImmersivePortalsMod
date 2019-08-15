package com.qouteall.immersive_portals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class CHelper {
    public static PlayerListEntry getClientPlayerListEntry() {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(
            MinecraftClient.getInstance().player.getGameProfile().getId()
        );
    }
}
