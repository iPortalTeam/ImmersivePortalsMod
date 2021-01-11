package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ducks.IEChunkTicketManager;
import com.qouteall.immersive_portals.ducks.IEServerChunkManager;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.ChunkPos;

import java.util.Comparator;
import java.util.WeakHashMap;

public class MyLoadingTicket {
    public static final ChunkTicketType<ChunkPos> portalLoadingTicketType =
        ChunkTicketType.create("imm_ptl", Comparator.comparingLong(ChunkPos::toLong));
    
    public static final ChunkTicketType<ChunkPos> temporalLoadingTicketType =
        ChunkTicketType.create(
            "imm_ptl_temportal",
            Comparator.comparingLong(ChunkPos::toLong),
            300//15 seconds
        );
    
    private static ChunkTicketManager getTicketManager(ServerWorld world) {
        return ((IEServerChunkManager) world.getChunkManager()).getTicketManager();
    }
    
    public static final WeakHashMap<ServerWorld, LongSortedSet>
        loadedChunkRecord = new WeakHashMap<>();
    
    private static boolean hasOtherChunkTicket(ServerWorld world, ChunkPos chunkPos) {
        SortedArraySet<ChunkTicket<?>> chunkTickets =
            ((IEChunkTicketManager) getTicketManager(world))
                .portal_getTicketSet(chunkPos.toLong());
        return chunkTickets.stream().anyMatch(t -> t.getType() != portalLoadingTicketType);
    }
    
    public static void addTicketIfNotLoaded(ServerWorld world, ChunkPos chunkPos) {
        boolean isNewlyAdded = getRecord(world).add(chunkPos.toLong());
        if (isNewlyAdded) {
            getTicketManager(world).addTicket(
                portalLoadingTicketType, chunkPos, getLoadingRadius(), chunkPos
            );
        }
    }
    
    public static void removeTicket(ServerWorld world, ChunkPos chunkPos) {
        boolean isNewlyRemoved = getRecord(world).remove(chunkPos.toLong());
        
        if (isNewlyRemoved) {
            getTicketManager(world).removeTicket(
                portalLoadingTicketType, chunkPos, getLoadingRadius(), chunkPos
            );
        }
    }
    
    public static int getLoadingRadius() {
        if (Global.activeLoading) {
            return 2;
        }
        else {
            return 1;
        }
    }
    
    public static LongSortedSet getRecord(ServerWorld world) {
        return loadedChunkRecord.computeIfAbsent(
            world, k -> new LongLinkedOpenHashSet()
        );
    }
    
    public static void loadTemporally(ServerWorld world, ChunkPos chunkPos) {
        getTicketManager(world).removeTicket(
            temporalLoadingTicketType, chunkPos, 2, chunkPos
        );
    }
    
    public static void loadTemporally(ServerWorld world, ChunkPos centerChunkPos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                loadTemporally(
                    world,
                    new ChunkPos(centerChunkPos.x + dx, centerChunkPos.z + dz)
                );
            }
        }
    }
}
