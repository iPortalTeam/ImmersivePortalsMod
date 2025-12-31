package qouteall.imm_ptl.core.chunk_loading;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongPredicate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ChunkTaskPriorityQueue;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IEDistanceManager;
import qouteall.imm_ptl.core.ducks.IEServerChunkCache;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.RateStat;

/**
 * Each {@link ImmPtlChunkTickets} manages ImmPtl chunk ticket for one dimension.
 * <p>
 * It re-implements player chunk loading throttling which is much simpler than vanilla's.
 * In vanilla, each chunk that get loaded by player has a chunk ticket.
 * The chunk tickets are not added immediately, but added by a throttled mechanism.
 * The throttling will reduce the world generation and chunk loading workload when the player moves fast,
 * and prioritize the chunks near player.
 * <p>
 * In vanilla, it uses {@link ChunkTaskPriorityQueue} that has 4 slots of "acquired" chunk positions.
 * If the acquired chunk slots are full, it will stop processing task, until a slot releases.
 * The {@link net.minecraft.server.level.ThrottlingChunkTaskDispatcher} uses a message queue
 * to coordinate ticket acquisition and release for player chunk loading.
 * The chunk positions to release are passed into {@link DistanceManager#ticketsToRelease}.
 * A callback for sending message for releasing will be added to these chunk's future.
 */
@SuppressWarnings("JavadocReference")
public class ImmPtlChunkTickets {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final TicketType TICKET_TYPE = new TicketType(
        TicketType.NO_TIMEOUT,
        TicketType.FLAG_PERSIST
            | TicketType.FLAG_LOADING
            | TicketType.FLAG_SIMULATION
            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE
    );
    
    // for debugging
    @SuppressWarnings("FieldMayBeFinal")
    private static boolean enableDebugRateStat = false;
    private static final RateStat debugRateStat = new RateStat("imm_ptl_chunk_ticket");
    
    // the fields of ImmPtlChunkTickets should avoid referencing ServerLevel
    public static final WeakHashMap<ServerLevel, ImmPtlChunkTickets> BY_DIMENSION = new WeakHashMap<>();
    
    public static void init() {
        Registry.register(
            BuiltInRegistries.TICKET_TYPE,
            McHelper.newIdentifier("immersive_portals", "imm_ptl_chunk_ticket"),
            TICKET_TYPE
        );
        
        DimensionAPI.SERVER_PRE_REMOVE_DIMENSION_EVENT.register(
            ImmPtlChunkTickets::onDimensionRemove
        );
        
        IPGlobal.SERVER_CLEANUP_EVENT.register(ImmPtlChunkTickets::cleanup);
    }
    
    public static class ChunkTicketInfo {
        public int lastUpdateGeneration;
        public int distanceToSource;
        
        public ChunkTicketInfo(int lastUpdateGeneration, int distanceToSource) {
            this.lastUpdateGeneration = lastUpdateGeneration;
            this.distanceToSource = distanceToSource;
        }
    }
    
    private final Long2ObjectOpenHashMap<ChunkTicketInfo> chunkPosToTicketInfo = new Long2ObjectOpenHashMap<>();
    
    private final ArrayList<LongLinkedOpenHashSet> chunksToAddTicketByDistance = new ArrayList<>();
    
    private final LongOpenHashSet waitingForLoading = new LongOpenHashSet();
    
    private boolean isValid = true;
    
    public final int throttlingLimit = 4;
    
    private ImmPtlChunkTickets() {
    
    }
    
    // it takes in world instead of dimension id, to ensure dimension really exists
    public static ImmPtlChunkTickets get(ServerLevel world) {
        return BY_DIMENSION.computeIfAbsent(world, k -> new ImmPtlChunkTickets());
    }
    
    public void markForLoading(long chunkPos, int distanceToSource, int generation) {
        Validate.isTrue(distanceToSource >= 0);
        
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
    
    public void tick(ServerLevel world) {
        flushThrottling(world);
    }
    
    /**
     * This method is called during ticking and {@link DistanceManager#runAllUpdates(ChunkMap)} .
     * <p>
     * Only calling this method during ticking will make it throttled too slow.
     * <p>
     * This method uses the chunk holder's future, so it should be called after
     * {@link DistanceManager#runAllUpdates(ChunkMap)}
     * (as it calls {@link ChunkHolder#updateFutures(ChunkMap, Executor)}).
     * Before updating the future, the chunk's entity ticking future may be a future that immediately returns {@link ChunkHolder.ChunkLoadingFailure} result.
     * Each task to {@link net.minecraft.server.level.ServerChunkCache.MainThreadExecutor} will trigger
     * {@link DistanceManager#runAllUpdates(ChunkMap)}.
     */
    public void flushThrottling(ServerLevel world) {
        if (Thread.currentThread() != ((IEWorld) world).portal_getThread()) {
            LOGGER.error("Called in a non-server-main (or server-world) thread.", new Throwable());
            return;
        }
        
        if (enableDebugRateStat) {
            debugRateStat.update();
        }
        
        if (!isValid) {
            LOGGER.error("flushing when invalid {}", world);
            return;
        }
        
        if (!world.getServer().isRunning()) {
            // important: don't add chunk ticket when server is saving
            // https://github.com/iPortalTeam/ImmersivePortalsMod/issues/1455
            return;
        }
        
        DistanceManager distanceManager = getDistanceManager(world);
        Executor mainThreadExecutor = ((qouteall.imm_ptl.core.mixin.common.chunk_sync.IEDistanceManager) distanceManager).ip_getMainThreadExecutor();
        
        // clear the already loaded chunks
        waitingForLoading.removeIf((long chunkPos) -> {
            ChunkHolder chunkHolder = getChunkHolder(world, chunkPos);
            if (chunkHolder == null) {
                return true;
            }
            
            ChunkResult<LevelChunk> resultNow = chunkHolder.getFullChunkFuture()
                .getNow(null);
            
            if (resultNow == null) {
                return false;
            }
            
            if (!resultNow.isSuccess()) {
                LOGGER.error(
                    "Chunk loading failure {} {} {}",
                    world, new ChunkPos(chunkPos), resultNow
                );
            }
            
            return true;
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
                    }
                    else {
                        LOGGER.warn("Chunk {} is not in the queue", new ChunkPos(chunkPos));
                    }
                }
            }
        }
    }
    
/*     private static void addTicket(DistanceManager distanceManager, long chunkPos) {
        if (!IPConfig.getConfig().enableImmPtlChunkLoading) {
            return;
        }
        
        ChunkPos chunkPosObj = new ChunkPos(chunkPos);
        TicketStorage ticketStorage =
            ((qouteall.imm_ptl.core.mixin.common.chunk_sync.IEDistanceManager) distanceManager)
                .ip_getTicketStorage();
        ticketStorage.addTicketWithRadius(
            TICKET_TYPE, chunkPosObj, getLoadingRadius()
        );
        
        if (enableDebugRateStat) {
            debugRateStat.hit();
        }
    } */

        
    private static void addTicket(DistanceManager distanceManager, long chunkPos) {
        // TEMP DEBUG: non bloccare il chunk loading
        // if (!IPConfig.getConfig().enableImmPtlChunkLoading) {
        //     return;
        // }

        ChunkPos chunkPosObj = new ChunkPos(chunkPos);
        TicketStorage ticketStorage =
            ((qouteall.imm_ptl.core.mixin.common.chunk_sync.IEDistanceManager) distanceManager)
                .ip_getTicketStorage();
        ticketStorage.addTicketWithRadius(
            TICKET_TYPE, chunkPosObj, getLoadingRadius()
        );

        if (enableDebugRateStat) {
            debugRateStat.hit();
        }
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
                waitingForLoading.remove(chunkPos);
                
                boolean pendingTicketAdding = getQueueByDistance(ticketInfo.distanceToSource)
                    .remove(chunkPos);
                
                if (!pendingTicketAdding) {
                    ChunkPos chunkPosObj = new ChunkPos(chunkPos);
                    TicketStorage ticketStorage =
                        ((qouteall.imm_ptl.core.mixin.common.chunk_sync.IEDistanceManager) distanceManager)
                            .ip_getTicketStorage();
                    ticketStorage.removeTicketWithRadius(
                        TICKET_TYPE, chunkPosObj, getLoadingRadius()
                    );
                }
                return true;
            }
            else {
                return false;
            }
        });
    }
    
    public int getLoadedChunkNum() {
        return chunkPosToTicketInfo.size();
    }
    
    public static void onDimensionRemove(ServerLevel world) {
        ImmPtlChunkTickets dimTicketManager = BY_DIMENSION.remove(world);
        
        if (dimTicketManager == null) {
            return;
        }
        
        removeAllTicketsInWorld(world, dimTicketManager);
    }
    
    private static void removeAllTicketsInWorld(ServerLevel world, ImmPtlChunkTickets dimTicketManager) {
        DistanceManager ticketManager = getDistanceManager(world);
        TicketStorage ticketStorage =
            ((qouteall.imm_ptl.core.mixin.common.chunk_sync.IEDistanceManager) ticketManager)
                .ip_getTicketStorage();
        
        dimTicketManager.chunkPosToTicketInfo.keySet().forEach((long pos) -> {
            List<Ticket> tickets = ((IEDistanceManager) getDistanceManager(world))
                .portal_getTicketSet(pos);
            
            // avoid removing ticket when iterating the ticket set
            List<Ticket> toRemove = tickets.stream()
                .filter(t -> t.getType() == TICKET_TYPE).toList();
            
            ChunkPos chunkPos = new ChunkPos(pos);
            for (Ticket ticket : toRemove) {
                ticketStorage.removeTicket(ticket, chunkPos);
            }
        });
        
        dimTicketManager.isValid = false;
    }
    
    public static int getLoadingRadius() {
        if (IPGlobal.activeLoading) {
            return 2;
        }
        else {
            return 1;
        }
    }
    
    public static ChunkHolder getChunkHolder(ServerLevel world, long chunkPos) {
        return ((IEChunkMap) (world.getChunkSource()).chunkMap).ip_getChunkHolder(chunkPos);
    }
    
    public static DistanceManager getDistanceManager(ServerLevel world) {
        return ((IEServerChunkCache) world.getChunkSource()).ip_getDistanceManager();
    }
    
    private static void cleanup(MinecraftServer server) {
        for (ImmPtlChunkTickets immPtlChunkTickets : BY_DIMENSION.values()) {
            immPtlChunkTickets.isValid = false;
        }
        BY_DIMENSION.clear();
    }
}
