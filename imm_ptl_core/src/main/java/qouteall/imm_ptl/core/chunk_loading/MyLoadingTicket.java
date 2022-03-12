package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEChunkTicketManager;
import qouteall.imm_ptl.core.ducks.IEServerChunkManager;
import qouteall.imm_ptl.core.ducks.IEServerWorld;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;

import java.util.Comparator;
import java.util.WeakHashMap;

public class MyLoadingTicket {
    public static final TicketType<ChunkPos> portalLoadingTicketType =
        TicketType.create("imm_ptl", Comparator.comparingLong(ChunkPos::toLong));
    
    public static final TicketType<ChunkPos> temporalLoadingTicketType =
        TicketType.create(
            "imm_ptl_temportal",
            Comparator.comparingLong(ChunkPos::toLong),
            300//15 seconds
        );
    
    public static DistanceManager getTicketManager(ServerLevel world) {
        return ((IEServerChunkManager) world.getChunkSource()).getTicketManager();
    }
    
    public static final WeakHashMap<ServerLevel, LongSortedSet>
        loadedChunkRecord = new WeakHashMap<>();
    
    private static boolean hasOtherChunkTicket(ServerLevel world, ChunkPos chunkPos) {
        SortedArraySet<Ticket<?>> chunkTickets =
            ((IEChunkTicketManager) getTicketManager(world))
                .portal_getTicketSet(chunkPos.toLong());
        return chunkTickets.stream().anyMatch(t -> t.getType() != portalLoadingTicketType);
    }
    
    public static void addTicketIfNotLoaded(ServerLevel world, ChunkPos chunkPos) {
        boolean isNewlyAdded = getRecord(world).add(chunkPos.toLong());
        if (isNewlyAdded) {
            getTicketManager(world).addRegionTicket(
                portalLoadingTicketType, chunkPos, getLoadingRadius(), chunkPos
            );
            PersistentEntitySectionManager<Entity> entityManager = ((IEServerWorld) world).ip_getEntityManager();
        }
    }
    
    public static void removeTicketIfPresent(ServerLevel world, ChunkPos chunkPos) {
        boolean isNewlyRemoved = getRecord(world).remove(chunkPos.toLong());
        
        if (isNewlyRemoved) {
            getTicketManager(world).removeRegionTicket(
                portalLoadingTicketType, chunkPos, getLoadingRadius(), chunkPos
            );
        }
    }
    
    public static int getLoadingRadius() {
        if (IPGlobal.activeLoading) {
            return 2;
        }
        else {
            return 1;
        }
    }
    
    public static LongSortedSet getRecord(ServerLevel world) {
        return loadedChunkRecord.computeIfAbsent(
            world, k -> new LongLinkedOpenHashSet()
        );
    }
    
    public static void loadTemporally(ServerLevel world, ChunkPos chunkPos) {
        getTicketManager(world).removeRegionTicket(
            temporalLoadingTicketType, chunkPos, 2, chunkPos
        );
    }
    
    public static void loadTemporally(ServerLevel world, ChunkPos centerChunkPos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                loadTemporally(
                    world,
                    new ChunkPos(centerChunkPos.x + dx, centerChunkPos.z + dz)
                );
            }
        }
    }
    
    public static void onDimensionRemove(ResourceKey<Level> dimension) {
        ServerLevel world = McHelper.getServerWorld(dimension);
        
        LongSortedSet longs = loadedChunkRecord.get(world);
        if (longs == null) {
            return;
        }
        
        DistanceManager ticketManager = getTicketManager(world);
        
        longs.forEach((long pos) -> {
            ChunkPos chunkPos = new ChunkPos(pos);
            ticketManager.removeRegionTicket(
                portalLoadingTicketType, chunkPos, getLoadingRadius(), chunkPos
            );
        });
        
        loadedChunkRecord.remove(world);
    }
    
    public static void init() {
        DynamicDimensionsImpl.beforeRemovingDimensionSignal.connect(MyLoadingTicket::onDimensionRemove);
    }
}
