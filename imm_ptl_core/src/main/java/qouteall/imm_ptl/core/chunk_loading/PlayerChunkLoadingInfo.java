package qouteall.imm_ptl.core.chunk_loading;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Set;

/**
 * Per-player chunk-loading related info.
 * Also do chunk packet sending throttling {@link PlayerChunkSender}
 */
public class PlayerChunkLoadingInfo {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Gets cleared in {@link NewChunkTrackingGraph#updateForPlayer(ServerPlayer)} and re-calculated
     */
    public final Set<ResourceKey<Level>> visibleDimensions = new ObjectOpenHashSet<>();
    
    /**
     * Per-player chunk loaders. Added and removed via the API.
     */
    public final ArrayList<ChunkLoader> additionalChunkLoaders = new ArrayList<>();
    
    public final ArrayList<ArrayDeque<NewChunkTrackingGraph.PlayerWatchRecord>> distanceToPendingChunks =
        new ArrayList<>();
    
    public int loadedChunks = 0;
    
    // normally chunk loading will update following to an interval
    // but if this is true, it will immediately update next tick
    public boolean shouldUpdateImmediately = false;
    
    public PerformanceLevel performanceLevel = PerformanceLevel.bad;
    
    /**
     * Do similar functionality as {@link PlayerChunkSender},
     * but for multi-dim and non-near-loading-only
     */
    public final boolean isMemoryConnection;
    private float desiredChunksPerTick = 9.0F;
    private float batchQuota;
    private int unacknowledgedBatches;
    private int maxUnacknowledgedBatches = 1;
    
    public PlayerChunkLoadingInfo(boolean isMemoryConnection) {
        this.isMemoryConnection = isMemoryConnection;
    }
    
    /**
     * one chunk may mark pending loading multiple times with different distanceToSource
     */
    public void markPendingLoading(NewChunkTrackingGraph.PlayerWatchRecord record) {
        Helper.arrayListComputeIfAbsent(
            distanceToPendingChunks,
            record.distanceToSource,
            ArrayDeque::new
        ).add(record);
    }
    
    /**
     * {@link PlayerChunkSender#sendNextChunks(ServerPlayer)}
     */
    @IPVanillaCopy
    public void doChunkSending(ServerPlayer serverPlayer) {
        if (this.unacknowledgedBatches >= this.maxUnacknowledgedBatches) {
            return;
        }
        
        this.batchQuota = Math.min(
            this.batchQuota + this.desiredChunksPerTick,
            Math.max(1.0F, this.desiredChunksPerTick)
        );
        
        if (this.batchQuota < 1.0F) {
            return;
        }
        
        ServerGamePacketListenerImpl connection = serverPlayer.connection;
        
        int maxSendNum = (int) Math.floor(batchQuota);
        Validate.isTrue(maxSendNum != 0);
        
        int sentNum = 0;
        for (ArrayDeque<NewChunkTrackingGraph.PlayerWatchRecord> queue : distanceToPendingChunks) {
            if (queue == null || queue.isEmpty()) {
                continue;
            }
            
            NewChunkTrackingGraph.PlayerWatchRecord record = queue.pollFirst();
            
            // chunk unloaded, skip
            if (!record.isValid) {
                continue;
            }
            
            // already loaded to player, skip
            if (record.isLoadedToPlayer) {
                continue;
            }
            
            ServerLevel world = MiscHelper.getServer().getLevel(record.dimension);
            if (world == null) {
                LOGGER.error(
                    "Missing dimension when flushing pending loading {}", record.dimension.location()
                );
                continue;
            }
            
            ChunkMap chunkMap = world.getChunkSource().chunkMap;
            ChunkHolder chunkHolder = ((IEChunkMap) chunkMap).ip_getChunkHolder(record.chunkPos);
            
            if (chunkHolder == null) {
                continue;
            }
            
            LevelChunk tickingChunk = chunkHolder.getTickingChunk();
            
            // skip that chunk if not yet loaded
            if (tickingChunk == null) {
                continue;
            }
            
            record.isLoadedToPlayer = true;
            
            if (sentNum == 0) {
                ++this.unacknowledgedBatches;
                connection.send(new ClientboundChunkBatchStartPacket());
            }
            sentNum++;
            
            sendChunkPacket(
                connection, world, tickingChunk
            );
            
            if (sentNum >= maxSendNum) {
                break;
            }
        }
        
        if (sentNum != 0) {
            connection.send(new ClientboundChunkBatchFinishedPacket(sentNum));
        }
        
        this.batchQuota -= (float) sentNum;
    }
    
    /**
     * {@link PlayerChunkSender#sendChunk(ServerGamePacketListenerImpl, ServerLevel, LevelChunk)}
     */
    @IPVanillaCopy
    private static void sendChunkPacket(
        ServerGamePacketListenerImpl serverGamePacketListenerImpl,
        ServerLevel serverLevel,
        LevelChunk levelChunk
    ) {
        serverGamePacketListenerImpl.send(
            PacketRedirection.createRedirectedMessage(
                serverLevel.getServer(),
                serverLevel.dimension(),
                new ClientboundLevelChunkWithLightPacket(
                    levelChunk, serverLevel.getLightEngine(), null, null
                )
            )
        );
    }
    
    /**
     * {@link PlayerChunkSender#onChunkBatchReceivedByClient(float)}
     */
    @IPVanillaCopy
    public void onChunkBatchReceivedByClient(float clientDesiredChunkPerTick) {
        --this.unacknowledgedBatches;
        this.desiredChunksPerTick = Double.isNaN((double)clientDesiredChunkPerTick) ?
            0.01F : Mth.clamp(clientDesiredChunkPerTick, 0.01F, 64.0F);
        if (this.unacknowledgedBatches == 0) {
            this.batchQuota = 1.0F;
        }
        
        this.maxUnacknowledgedBatches = 10;
    }
}
