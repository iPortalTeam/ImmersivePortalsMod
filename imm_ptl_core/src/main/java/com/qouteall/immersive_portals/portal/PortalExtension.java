package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.WeakHashMap;

// the additional features of a portal
public class PortalExtension {
    
    /**
     * @param portal
     * @return the portal extension object
     */
    public static PortalExtension get(Portal portal) {
        if (portal.extension == null) {
            portal.extension = new PortalExtension();
        }
        return portal.extension;
    }
    
    public static void init() {
        Portal.clientPortalTickSignal.connect(portal -> {
            get(portal).tick(portal);
            
        });
        
        Portal.serverPortalTickSignal.connect(portal -> {
            get(portal).tick(portal);
            
        });
        
        Portal.readPortalDataSignal.connect((portal, tag) -> {
            get(portal).readFromNbt(tag);
        });
        
        Portal.writePortalDataSignal.connect((portal, tag) -> {
            get(portal).writeToNbt(tag);
        });
    }
    
    /**
     * If positive, the player that's touching the portal will be accelerated
     * If negative, the player that's touching the portal and moving quickly will
     *  be decelerated
     */
    public double motionAffinity = 0;
    
    /**
     * If true, when the player comes out from the portal and get stuck in block
     * the player will be smoothly levitated to avoid falling through floor
     */
    public boolean adjustPositionAfterTeleport = false;
    
    private static class PlayerPortalVisibility {
        public long lastVisibleTime = 0;
        public double currentCap = 0;
        public int targetCap = 0;
        
        public void updateEverySecond() {
            double targetCapSq = targetCap * targetCap;
            double currentCapSq = currentCap * currentCap;
            
            if (currentCapSq < targetCapSq) {
                currentCapSq = Math.min(
                    currentCapSq + (targetCapSq / 12),
                    targetCapSq
                );
            }
            else {
                currentCapSq = Math.max(
                    currentCapSq - (targetCapSq / 12),
                    targetCapSq
                );
            }
            currentCap = Math.sqrt(currentCapSq);
        }
    }
    
    private WeakHashMap<ServerPlayerEntity, PlayerPortalVisibility> playerLoadStatus;
    
    public PortalExtension() {
    
    }
    
    private void readFromNbt(CompoundTag compoundTag) {
        if (compoundTag.contains("motionAffinity")) {
            motionAffinity = compoundTag.getDouble("motionAffinity");
        }
        else {
            motionAffinity = 0;
        }
        if (compoundTag.contains("adjustPositionAfterTeleport")) {
            adjustPositionAfterTeleport = compoundTag.getBoolean("adjustPositionAfterTeleport");
        }
        else {
            adjustPositionAfterTeleport = false;
        }
    }
    
    private void writeToNbt(CompoundTag compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
        compoundTag.putBoolean("adjustPositionAfterTeleport", adjustPositionAfterTeleport);
    }
    
    private void tick(Portal portal) {
        if (portal.world.isClient()) {
            tickClient(portal);
        }
        else {
            if (playerLoadStatus == null) {
                playerLoadStatus = new WeakHashMap<>();
            }
            playerLoadStatus.entrySet().removeIf(e -> e.getKey().removed);
            
            if (portal.world.getTime() % 20 == 1) {
                for (PlayerPortalVisibility value : playerLoadStatus.values()) {
                    value.updateEverySecond();
                }
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient(Portal portal) {
    
    }
    
    public int refreshAndGetLoadDistanceCap(Portal portal, ServerPlayerEntity player, int currentCap) {
        if (playerLoadStatus == null) {
            playerLoadStatus = new WeakHashMap<>();
        }
        
        PlayerPortalVisibility rec = playerLoadStatus.computeIfAbsent(
            player, k -> new PlayerPortalVisibility()
        );
        
        final int dropTimeout = NewChunkTrackingGraph.updateInterval * 2;
        
        long worldTime = portal.world.getTime();
        
        long timePassed = Math.abs(worldTime - rec.lastVisibleTime);
        if (timePassed > dropTimeout) {
            // not loaded for sometime and reload, reset all
            rec.targetCap = currentCap;
            rec.currentCap = 0;
        }
        else if (timePassed == 0) {
            // being checked the second time in this turn
            rec.targetCap = Math.max(rec.targetCap, currentCap);
        }
        else {
            // being checked the first time in this turn
            rec.targetCap = currentCap;
        }
        
        rec.lastVisibleTime = portal.world.getTime();
        
        return (int) Math.round(rec.currentCap);
    }
}
