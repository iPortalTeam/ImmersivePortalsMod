package qouteall.imm_ptl.core.chunk_loading;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkTaskPriorityQueue;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEChunkTicketManager;
import qouteall.imm_ptl.core.ducks.IEServerChunkManager;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.mixin.common.chunk_sync.IEDistanceManager;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

public class MyLoadingTicket {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final TicketType<ChunkPos> portalLoadingTicketType =
        TicketType.create("imm_ptl", Comparator.comparingLong(ChunkPos::toLong));
    
    public static DistanceManager getDistanceManager(ServerLevel world) {
        return ((IEServerChunkManager) world.getChunkSource()).ip_getDistanceManager();
    }
    
    public static class ChunkTicketInfo {
        public int lastUpdateGeneration;
        public int distanceToSource;
        
        public ChunkTicketInfo(int lastUpdateGeneration, int distanceToSource) {
            this.lastUpdateGeneration = lastUpdateGeneration;
            this.distanceToSource = distanceToSource;
        }
    }
    
    /**
     * Re-implement player chunk loading throttling which is much simpler than vanilla's.
     * In vanilla, each chunk that get loaded by player has a chunk ticket.
     * The chunk tickets are not added immediately, but added by a throttled mechanism.
     * The throttling will reduce the world generation and chunk loading workload when the player moves fast,
     * and prioritize the chunks near player.
     * <p>
     * In vanilla, it uses {@link ChunkTaskPriorityQueue} that has 4 slots of "acquired" chunk positions.
     * If the acquired chunk slots are full, it will stop processing task, until a slot releases.
     * The {@link ChunkTaskPriorityQueueSorter} uses a {@link ProcessorMailbox}
     * (the mailbox is similar to a one-thread thread pool but uses threads from the worker thread pool)
     * to do a lot of unnecessary message-passing (it enqueues at least 5 messages just to add one ticket).
     * (It is very over-engineered and contains a lot of unnecessary complexity.)
     * In {@link DistanceManager.PlayerTicketTracker} it sends message for acquiring and releasing.
     * The chunk positions to release are passed into {@link DistanceManager#ticketsToRelease}.
     * A callback for sending message for releasing will be added to these chunk's future.
     */
    public static class DimTicketManager {
        // avoid referencing ServerLevel in fields
        // otherwise the weak hash map will not auto clear
    
        public final Long2ObjectOpenHashMap<ChunkTicketInfo> chunkPosToTicketInfo = new Long2ObjectOpenHashMap<>();
        
        public final ArrayList<LongLinkedOpenHashSet> chunksToAddTicketByDistance = new ArrayList<>();
        
        public final LongOpenHashSet waitingForLoading = new LongOpenHashSet();
        
        public final int throttlingLimit = 4;
        
        public DimTicketManager() {
        
        }
        
        public void markForLoading(long chunkPos, int distanceToSource, int generation) {
            Validate.isTrue(distanceToSource >= 0 && distanceToSource <= 32);
            
            ChunkTicketInfo info = chunkPosToTicketInfo.get(chunkPos);
            
            if (info == null) {
                info = new ChunkTicketInfo(generation, distanceToSource);
                chunkPosToTicketInfo.put(chunkPos, info);
                getQueueByDistance(distanceToSource).add(chunkPos);
            }
            else {
                if (generation != info.lastUpdateGeneration) {
                    info.lastUpdateGeneration = generation;
                    int oldDistanceToSource = info.distanceToSource;
                    info.distanceToSource = distanceToSource;
                    if (getQueueByDistance(oldDistanceToSource).remove(chunkPos)) {
                        getQueueByDistance(distanceToSource).add(chunkPos);
                    }
                }
                else {
                    if (distanceToSource < info.distanceToSource) {
                        int oldDistanceToSource = info.distanceToSource;
                        info.distanceToSource = distanceToSource;
                        if (getQueueByDistance(oldDistanceToSource).remove(chunkPos)) {
                            getQueueByDistance(distanceToSource).add(chunkPos);
                        }
                    }
                }
            }
        }
        
        private LongLinkedOpenHashSet getQueueByDistance(int distanceToSource) {
            return Helper.arrayListComputeIfAbsent(
                chunksToAddTicketByDistance,
                distanceToSource,
                LongLinkedOpenHashSet::new
            );
        }
        
        // this is run in two cases:
        // 1. ticking
        // 2. triggered from callback of a chunk loading future
        // (only flush during ticking will make it throttled too slow)
        public void flushThrottling(ServerLevel world) {
            Validate.isTrue(world.getServer().isSameThread(), "should run on server main thread");
            
            DistanceManager distanceManager = getDistanceManager(world);
            Executor mainThreadExecutor = ((IEDistanceManager) distanceManager).ip_getMainThreadExecutor();
            
            // clear the already loaded chunks
            waitingForLoading.removeIf((long chunkPos) -> {
                ChunkHolder chunkHolder = getChunkHolder(world, chunkPos);
                if (chunkHolder == null) {
                    LOGGER.error("Missing chunk holder {} {}", world, new ChunkPos(chunkPos));
                    return true;
                }
                
                Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> resultNow =
                    chunkHolder.getEntityTickingChunkFuture().getNow(null);
                
                return resultNow != null;
            });
            
            // flush the pending-add-ticket queues
            for (LongLinkedOpenHashSet queue : chunksToAddTicketByDistance) {
                if (queue != null) {
                    while (!queue.isEmpty()) {
                        if (waitingForLoading.size() >= throttlingLimit) {
                            return;
                        }
                        
                        long chunkPos = queue.removeFirstLong();
                        if (chunkPosToTicketInfo.containsKey(chunkPos)) {
                            addTicket(distanceManager, chunkPos);
                            
                            waitingForLoading.add(chunkPos);
                            
                            // add callback to the chunk loading future
                            ChunkHolder chunkHolder = getChunkHolder(world, chunkPos);
                            if (chunkHolder != null) {
                                addCallbackToFuture(world, mainThreadExecutor, chunkHolder);
                            }
                            else {
                                // sometimes the chunk holder are only present after adding ticket
                                // so add callback to the future later
                                mainThreadExecutor.execute(() -> {
                                    ChunkHolder chunkHolder2 = getChunkHolder(world, chunkPos);
                                    if (chunkHolder2 != null) {
                                        addCallbackToFuture(world, mainThreadExecutor, chunkHolder2);
                                    }
                                    else {
                                        LOGGER.error(
                                            "Missing chunk holder after adding ticket {} {}",
                                            world, new ChunkPos(chunkPos)
                                        );
                                    }
                                });
                            }
                        }
                        else {
                            LOGGER.warn("Chunk {} is not in the queue", new ChunkPos(chunkPos));
                        }
                    }
                }
            }
        }
        
        private static void addTicket(DistanceManager distanceManager, long chunkPos) {
            if (!IPConfig.getConfig().enableImmPtlChunkLoading) {
                return;
            }
            
            ChunkPos chunkPosObj = new ChunkPos(chunkPos);
            distanceManager.addRegionTicket(
                portalLoadingTicketType, chunkPosObj, getLoadingRadius(), chunkPosObj
            );
        }
        
        private void addCallbackToFuture(
            ServerLevel world, Executor mainThreadExecutor, ChunkHolder chunkHolder
        ) {
            chunkHolder.getEntityTickingChunkFuture()
                .thenAccept(e -> {
                    mainThreadExecutor.execute(() -> {
                        flushThrottling(world);
                    });
                });
        }
        
        private static ChunkHolder getChunkHolder(ServerLevel world, long chunkPos) {
            return ((IEThreadedAnvilChunkStorage) (world.getChunkSource()).chunkMap).ip_getChunkHolder(chunkPos);
        }
        
        public void purge(
            ServerLevel world,
            LongPredicate shouldKeepLoadingFunc
        ) {
            DistanceManager distanceManager = getDistanceManager(world);
            
            chunkPosToTicketInfo.long2ObjectEntrySet().removeIf(e -> {
                long chunkPos = e.getLongKey();
                ChunkTicketInfo ticketInfo = e.getValue();
                
                boolean keepLoading = shouldKeepLoadingFunc.test(chunkPos);
                
                if (!keepLoading) {
                    boolean pendingTicketAdding = getQueueByDistance(ticketInfo.distanceToSource)
                        .remove(chunkPos);
                    
                    if (!pendingTicketAdding) {
                        ChunkPos chunkPosObj = new ChunkPos(chunkPos);
                        distanceManager.removeRegionTicket(
                            portalLoadingTicketType, chunkPosObj, getLoadingRadius(), chunkPosObj
                        );
                    }
                    return true;
                }
                else {
                    return false;
                }
            });
        }
    }
    
    public static final WeakHashMap<ServerLevel, DimTicketManager>
        dimTicketManagerMap = new WeakHashMap<>();
    
    // it takes in world instead of dimension id, to ensure dimension really exists
    public static DimTicketManager getDimTicketManager(ServerLevel world) {
        return dimTicketManagerMap.computeIfAbsent(
            world,
            k -> new DimTicketManager()
        );
    }
    
    public static int getLoadingRadius() {
        if (IPGlobal.activeLoading) {
            return 2;
        }
        else {
            return 1;
        }
    }
    
    private static boolean hasOtherChunkTicket(ServerLevel world, ChunkPos chunkPos) {
        SortedArraySet<Ticket<?>> chunkTickets =
            ((IEChunkTicketManager) getDistanceManager(world))
                .portal_getTicketSet(chunkPos.toLong());
        return chunkTickets.stream().anyMatch(t -> t.getType() != portalLoadingTicketType);
    }
    
    public static void onDimensionRemove(ResourceKey<Level> dimension) {
        ServerLevel world = McHelper.getServerWorld(dimension);
        
        DimTicketManager dimTicketManager = dimTicketManagerMap.remove(world);
        
        if (dimTicketManager == null) {
            return;
        }
        
        removeAllTicketsInWorld(world, dimTicketManager);
    }
    
    private static void removeAllTicketsInWorld(ServerLevel world, DimTicketManager dimTicketManager) {
        DistanceManager ticketManager = getDistanceManager(world);
        
        dimTicketManager.chunkPosToTicketInfo.keySet().forEach((long pos) -> {
            SortedArraySet<Ticket<?>> tickets = ((IEChunkTicketManager) getDistanceManager(world))
                .portal_getTicketSet(pos);
            
            // avoid removing ticket when iterating the ticket set
            List<Ticket<?>> toRemove = tickets.stream()
                .filter(t -> t.getType() == portalLoadingTicketType).toList();
            
            ChunkPos chunkPos = new ChunkPos(pos);
            for (Ticket<?> ticket : toRemove) {
                ticketManager.removeRegionTicket(portalLoadingTicketType, chunkPos, ticket.getTicketLevel(), chunkPos);
            }
        });
    }
    
    public static void init() {
        DynamicDimensionsImpl.beforeRemovingDimensionSignal.connect(MyLoadingTicket::onDimensionRemove);
        
        IPGlobal.serverCleanupSignal.connect(dimTicketManagerMap::clear);
    }
}
