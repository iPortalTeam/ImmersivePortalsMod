package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

public class DubiousThings {
    public static void init() {
        ModMain.postClientTickSignal.connect(DubiousThings::tick);
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
            checkClientPlayerState();
        }
    }
    
    private static void doUpdateLight(ClientPlayerEntity player) {
        MinecraftClient.getInstance().getProfiler().push("my_light_update");
        MyClientChunkManager.updateLightStatus(player.world.getChunk(
            player.chunkX, player.chunkZ
        ));
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private static void checkClientPlayerState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != client.player.world) {
            Helper.err("Player world abnormal");
            //don't know how to fix it
        }
        Entity playerInWorld = client.world.getEntityById(client.player.getEntityId());
        if (playerInWorld != client.player) {
            Helper.err("Client Player Mismatch");
            if (playerInWorld instanceof ClientPlayerEntity) {
                client.player = ((ClientPlayerEntity) playerInWorld);
                Helper.log("Force corrected");
            }
            else {
                Helper.err("Non-player entity in client has duplicate id");
            }
        }
    }
}
