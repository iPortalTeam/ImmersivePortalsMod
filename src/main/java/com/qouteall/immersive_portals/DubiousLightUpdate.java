package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class DubiousLightUpdate {
    public static void init() {
        ModMain.postClientTickSignal.connect(DubiousLightUpdate::tick);
    }
    
    //fix light issue https://github.com/qouteall/ImmersivePortalsMod/issues/45
    //it's not an elegant solution
    //the issue could be caused by other things
    private static void tick() {
        ClientWorld world = MinecraftClient.getInstance().world;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (world == null) {
            return;
        }
        if (player == null) {
            return;
        }
        if (world.getTime() % 233 == 34) {
            doUpdateLight(player);
        }
    }
    
    private static void doUpdateLight(ClientPlayerEntity player) {
        MinecraftClient.getInstance().getProfiler().push("my_light_update");
        MyClientChunkManager.updateLightStatus(player.world.getChunk(
            player.chunkX, player.chunkZ
        ));
        MinecraftClient.getInstance().getProfiler().pop();
    }
}
