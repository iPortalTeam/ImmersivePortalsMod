package com.qouteall.immersive_portals.portal.extension;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.WeakHashMap;

// the additional features of a portal
public class PortalExtension {
    public double motionAffinity = 0;
    
    private static class PlayerPortalVisibility {
        public long startVisibleTime = 0;
        public long lastVisibleTime = 0;
    }
    
    private WeakHashMap<ServerPlayerEntity, PlayerPortalVisibility> playerLoadStatus;
    
    public PortalExtension() {
    
    }
    
    public void readFromNbt(CompoundTag compoundTag) {
        if (compoundTag.contains("motionAffinity")) {
            motionAffinity = compoundTag.getDouble("motionAffinity");
        }
    }
    
    public void writeToNbt(CompoundTag compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
    }
    
    public void tick(Portal portal) {
        if (portal.world.isClient()) {
            tickClient(portal);
        }
        else {
            if (playerLoadStatus == null) {
                playerLoadStatus = new WeakHashMap<>();
            }
            playerLoadStatus.entrySet().removeIf(e -> e.getKey().removed);
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient(Portal portal) {
    
    }
    
    public int refreshAndGetMaxLoadDistance(Portal portal, ServerPlayerEntity player) {
        if (playerLoadStatus == null) {
            playerLoadStatus = new WeakHashMap<>();
        }
        
        PlayerPortalVisibility rec = playerLoadStatus.computeIfAbsent(
            player, k -> new PlayerPortalVisibility()
        );
        
        final int dropTimeout = NewChunkTrackingGraph.updateInterval * 2;
        
        long worldTime = portal.world.getTime();
        
        if (Math.abs(worldTime - rec.lastVisibleTime) > dropTimeout) {
            rec.startVisibleTime = worldTime;
        }
        
        rec.lastVisibleTime = worldTime;
        
        long watchedTicks = rec.lastVisibleTime - rec.startVisibleTime;
        
        int watchedSeconds = ((int) (watchedTicks / 20));
    
        return watchedSeconds;
    }
}
