package com.qouteall.immersive_portals.render.lag_spike_fix;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

// intercept the load/unload packets and apply them bit by bit later
// to reduce lag spike
// has issues currently, disabled by default
@Environment(EnvType.CLIENT)
public class SmoothLoading {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static class WorldInfo {
        public WeakReference<ClientWorld> world;
        public List<Packet<ClientPlayPacketListener>> packets = new ArrayList<>();
        
        public WorldInfo(WeakReference<ClientWorld> world) {
            this.world = world;
        }
        
        public void removeChunkLoadingPackets(int cx, int cz) {
            this.packets.removeIf(p -> {
                if (p instanceof ChunkDataS2CPacket) {
                    ChunkDataS2CPacket pt = (ChunkDataS2CPacket) p;
                    if (pt.getX() == cx && pt.getZ() == cz) {
                        return true;
                    }
                }
                else if (p instanceof LightUpdateS2CPacket) {
                    LightUpdateS2CPacket pt = (LightUpdateS2CPacket) p;
                    if (pt.getChunkX() == cx && pt.getChunkZ() == cz) {
                        return true;
                    }
                }
                return false;
            });
        }
        
        public void removeChunkUnloadPackets(int cx, int cz) {
            packets.removeIf(p -> {
                if (p instanceof UnloadChunkS2CPacket) {
                    UnloadChunkS2CPacket pt = (UnloadChunkS2CPacket) p;
                    return pt.getX() == cx && pt.getZ() == cz;
                }
                return false;
            });
        }
        
        public void sortByDistanceToBarycenter() {
            long xSum = 0;
            long zSum = 0;
            
            for (Packet<ClientPlayPacketListener> packet : packets) {
                ChunkPos chunkPos = getChunkPosOf(packet);
                xSum += chunkPos.x;
                zSum += chunkPos.z;
            }
            
            int centerX = (int) (xSum / packets.size());
            int centerZ = (int) (zSum / packets.size());
            
            packets.sort(Comparator.comparingDouble((Packet<ClientPlayPacketListener> packet) -> {
                ChunkPos chunkPos = getChunkPosOf(packet);
                return Helper.getChebyshevDistance(
                    chunkPos.x, chunkPos.z,
                    centerX, centerZ
                );
            }).reversed());
        }
    }
    
    private static final WeakHashMap<ClientWorld, WorldInfo> data = new WeakHashMap<>();
    
    private static int coolDown = getInterval();
    
    public static void init() {
        ModMain.postClientTickSignal.connect(SmoothLoading::tick);
    }
    
    private static int getSplitRatio() {
        return 40;
    }
    
    private static WorldInfo getWorldInfo(ClientWorld world) {
        return data.computeIfAbsent(
            world, k -> new WorldInfo(new WeakReference<>(k))
        );
    }
    
    private static boolean isNearPlayer(ClientWorld world, int chunkX, int chunkZ) {
//        if (world != client.player.world) {
//            return false;
//        }
        return false;

//        return Helper.getChebyshevDistance(
//            chunkX, chunkZ,
//            client.player.chunkX, client.player.chunkZ
//        ) <= (client.options.viewDistance + 5);
    }
    
    // return true indicates that the packet is intercepted
    public static boolean filterPacket(ClientWorld world, Packet<ClientPlayPacketListener> packet) {
        if (!Global.smoothLoading) {
            return false;
        }
        
        if (packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket p = (ChunkDataS2CPacket) packet;
            if (!isNearPlayer(world, p.getX(), p.getZ())) {
                interceptPacket(world, packet);
                return true;
            }
        }
        else if (packet instanceof LightUpdateS2CPacket) {
            LightUpdateS2CPacket p = (LightUpdateS2CPacket) packet;
            if (!isNearPlayer(world, p.getChunkX(), p.getChunkZ())) {
                interceptPacket(world, packet);
                return true;
            }
        }
        else if (packet instanceof UnloadChunkS2CPacket) {
            UnloadChunkS2CPacket p = (UnloadChunkS2CPacket) packet;
            if (!isNearPlayer(world, p.getX(), p.getZ())) {
                interceptPacket(world, packet);
                return true;
            }
        }
        
        return false;
    }
    
    private static void interceptPacket(
        ClientWorld world,
        Packet<ClientPlayPacketListener> packet
    ) {
        WorldInfo worldInfo = getWorldInfo(world);
        
        if (packet instanceof UnloadChunkS2CPacket) {
            UnloadChunkS2CPacket packetT = (UnloadChunkS2CPacket) packet;
            worldInfo.removeChunkLoadingPackets(packetT.getX(), packetT.getZ());
        }
        else if (packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket packetT = (ChunkDataS2CPacket) packet;
            worldInfo.removeChunkUnloadPackets(packetT.getX(), packetT.getZ());
        }
        else if (packet instanceof LightUpdateS2CPacket) {
            LightUpdateS2CPacket packetT = (LightUpdateS2CPacket) packet;
            worldInfo.removeChunkUnloadPackets(packetT.getChunkX(), packetT.getChunkZ());
        }
        
        worldInfo.packets.add(packet);
    }
    
    private static void applyPacket(
        ClientWorld world,
        Packet<ClientPlayPacketListener> packet
    ) {
        MyNetworkClient.doProcessRedirectedMessage(world, packet);
    }
    
    private static void tick() {
        if (client.world == null) {
            return;
        }
        if (client.player == null) {
            return;
        }
        
        coolDown--;
        
        if (coolDown <= 0) {
            coolDown = getInterval();
            flushInterceptedPackets();
        }
    }
    
    public static void cleanUp() {
        data.clear();
    }
    
    private static void flushInterceptedPackets() {
        client.getProfiler().push("flush_intercepted_packets");
        data.forEach(SmoothLoading::flushPacketsForWorld);
        client.getProfiler().pop();
    }
    
    private static void flushPacketsForWorld(ClientWorld world, WorldInfo info) {
        if (info.packets.isEmpty()) {
            return;
        }
        
        info.sortByDistanceToBarycenter();
        
        int packets = (int) (info.packets.size() / ((double) getSplitRatio()));
        
        if (packets == 0) {
            packets = 1;
        }
        
        for (int i = 0; i < packets; i++) {
            Packet<ClientPlayPacketListener> lastPacket = info.packets.get(info.packets.size() - 1);
            info.packets.remove(info.packets.size() - 1);
            applyPacket(world, lastPacket);
        }
    }
    
    private static ChunkPos getChunkPosOf(Packet<ClientPlayPacketListener> packet) {
        if (packet instanceof ChunkDataS2CPacket) {
            return new ChunkPos(
                ((ChunkDataS2CPacket) packet).getX(),
                ((ChunkDataS2CPacket) packet).getZ()
            );
        }
        else if (packet instanceof LightUpdateS2CPacket) {
            return new ChunkPos(
                ((LightUpdateS2CPacket) packet).getChunkX(),
                ((LightUpdateS2CPacket) packet).getChunkZ()
            );
        }
        else if (packet instanceof UnloadChunkS2CPacket) {
            return new ChunkPos(
                ((UnloadChunkS2CPacket) packet).getX(),
                ((UnloadChunkS2CPacket) packet).getZ()
            );
        }
        throw new RuntimeException("oops");
    }
    
    private static int getInterval() {
        return 1;
    }
}
