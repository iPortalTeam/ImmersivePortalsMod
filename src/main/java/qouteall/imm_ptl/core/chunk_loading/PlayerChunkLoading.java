package qouteall.imm_ptl.core.chunk_loading;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.fabricmc.fabric.impl.attachment.sync.AttachmentChange;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Per-player chunk-loading related info.
 * Also do chunk packet sending throttling {@link PlayerChunkSender}
 */
@SuppressWarnings({"JavadocReference", "DanglingJavadoc", "UnstableApiUsage"})
public class PlayerChunkLoading {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Attiva con JVM arg: -Dimmptl.dbg.chunk_sync=true
    private static final boolean DBG_CHUNK_SYNC = Boolean.getBoolean("immptl.dbg.chunk_sync");

    // Per evitare spam: logga throttle solo quando cambia il valore di unacked in stato di throttle
    private int lastLoggedThrottleUnacked = Integer.MIN_VALUE;

    /**
     * Gets cleared in {@link ImmPtlChunkTracking#updateForPlayer(ServerPlayer)} and re-calculated
     */
    public final Set<ResourceKey<Level>> visibleDimensions = new ObjectOpenHashSet<>();

    /**
     * Per-player chunk loaders. Added and removed via the API.
     */
    public final ArrayList<ChunkLoader> additionalChunkLoaders = new ArrayList<>();

    public final ArrayList<ObjectArrayList<ImmPtlChunkTracking.PlayerWatchRecord>> distanceToPendingChunks =
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

    public PlayerChunkLoading(boolean isMemoryConnection) {
        this.isMemoryConnection = isMemoryConnection;
    }

    /**
     * one chunk may mark pending loading multiple times with different distanceToSource
     */
    public void markPendingLoading(ImmPtlChunkTracking.PlayerWatchRecord record) {
        Helper.arrayListComputeIfAbsent(
            distanceToPendingChunks,
            record.distanceToSource,
            ObjectArrayList::new
        ).add(record);
    }

    private void logThrottle(ServerPlayer player) {
        int pendingBuckets = 0;
        int pendingTotal = 0;

        for (var bucket : this.distanceToPendingChunks) {
            if (bucket != null && !bucket.isEmpty()) {
                pendingBuckets++;
                pendingTotal += bucket.size();
            }
        }

        LOGGER.info(
            "[ImmPtlDbg] THROTTLE player={} unacked={}/{} quota={} desiredCpt={} pendingBuckets={}/{} pendingTotal={}",
            player.getName().getString(),
            this.unacknowledgedBatches, this.maxUnacknowledgedBatches,
            this.batchQuota,
            this.desiredChunksPerTick,
            pendingBuckets, this.distanceToPendingChunks.size(),
            pendingTotal
        );
    }

    /**
     * {@link PlayerChunkSender#sendNextChunks(ServerPlayer)}
     */
    @IPVanillaCopy
    public void doChunkSending(ServerPlayer serverPlayer) {
        // THROTTLE: se il client non sta "ackando" i batch, non mandiamo altri chunk
        if (this.unacknowledgedBatches >= this.maxUnacknowledgedBatches) {
            if (DBG_CHUNK_SYNC && this.lastLoggedThrottleUnacked != this.unacknowledgedBatches) {
                logThrottle(serverPlayer);
                this.lastLoggedThrottleUnacked = this.unacknowledgedBatches;
            }
            return;
        } else {
            // appena torniamo sotto soglia, permettiamo un nuovo log al prossimo throttle
            this.lastLoggedThrottleUnacked = Integer.MIN_VALUE;
        }

        if (isMemoryConnection) {
            // comunque rate-limit: l'integrated server non è "gratis" lato client
            this.batchQuota = Math.min(this.batchQuota + this.desiredChunksPerTick, 64.0F);
            if (this.batchQuota < 1.0F) return;
        }
        else {
            this.batchQuota = Math.min(
                this.batchQuota + this.desiredChunksPerTick,
                Math.max(1.0F, this.desiredChunksPerTick)
            );
            if (this.batchQuota < 1.0F) return;
        }

        ServerGamePacketListenerImpl connection = serverPlayer.connection;
        MinecraftServer server = serverPlayer.level().getServer();
        Validate.notNull(server, "server is null");

        int maxSendNum = (int) Math.floor(batchQuota);
        Validate.isTrue(maxSendNum != 0);

        MutableInt sentNum = new MutableInt(0);
        for (var recs : distanceToPendingChunks) {
            if (recs == null || recs.isEmpty()) {
                continue;
            }

            if (sentNum.getValue() >= maxSendNum) {
                break;
            }

            Helper.removeIfWithEarlyExit(recs, (record, shouldStop) -> {
                // chunk unloaded, remove
                if (!record.isValid) {
                    return true;
                }

                // already loaded to player, remove
                if (record.isLoadedToPlayer) {
                    return true;
                }

                ServerLevel world = server.getLevel(record.dimension);
                if (world == null) {
                    LOGGER.error(
                        "Missing dimension when flushing pending loading {}",
                        record.dimension.identifier()
                    );
                    return true;
                }

                ChunkMap chunkMap = world.getChunkSource().chunkMap;
                LevelChunk chunkToSend = chunkMap.getChunkToSend(record.chunkPos);

                // skip that chunk if not yet loaded
                if (chunkToSend == null) {
                    return false;
                }

                record.isLoadedToPlayer = true;

                if (sentNum.getValue() == 0) {
                    ++this.unacknowledgedBatches;
                    connection.send(ClientboundChunkBatchStartPacket.INSTANCE);
                }
                sentNum.increment();

                sendChunkPacket(connection, world, chunkToSend);

                if (sentNum.getValue() >= maxSendNum) {
                    shouldStop.setValue(true);
                }

                return true; // remove from list
            });
        }

        if (sentNum.getValue() != 0) {
            connection.send(new ClientboundChunkBatchFinishedPacket(sentNum.getValue()));
        }

        this.batchQuota -= (float) sentNum.getValue();
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
        PacketRedirection.withForceRedirect(
            serverLevel,
            () -> {
                serverGamePacketListenerImpl.send(
                    new ClientboundLevelChunkWithLightPacket(
                        levelChunk, serverLevel.getLightEngine(), null, null
                    )
                );

                onSendPacket(serverGamePacketListenerImpl, levelChunk);
            }
        );
    }

    /**
     * Fabric API's mixin {@link net.fabricmc.fabric.mixin.attachment.ChunkDataSenderMixin}
     * is cancelled in {@link qouteall.imm_ptl.core.mixin.common.chunk_sync.MixinPlayerChunkSender}.
     * So manually implement it here.
     */
    @IPVanillaCopy
    private static void onSendPacket(ServerGamePacketListenerImpl listener, LevelChunk chunk) {
        ServerPlayer player = listener.player;

        List<AttachmentChange> changes = new ArrayList<>();
        ((AttachmentTargetImpl) chunk).fabric_computeInitialSyncChanges(player, changes::add);

        if (!changes.isEmpty()) {
            AttachmentChange.partitionAndSendPackets(changes, player);
        }
    }

    /**
     * {@link PlayerChunkSender#onChunkBatchReceivedByClient(float)}
     */
    @IPVanillaCopy
    public void onChunkBatchReceivedByClient(float clientDesiredChunkPerTick) {
        // Evita andare sotto zero se arriva qualcosa di strano
        if (this.unacknowledgedBatches > 0) {
            --this.unacknowledgedBatches;
        } else {
            this.unacknowledgedBatches = 0;
        }

        this.desiredChunksPerTick = Double.isNaN((double) clientDesiredChunkPerTick) ?
            0.01F : Mth.clamp(clientDesiredChunkPerTick, 0.01F, 64.0F);

        if (this.unacknowledgedBatches == 0) {
            this.batchQuota = 1.0F;
        }

        // lascia il tuo valore, ma ora lo vediamo anche nei log se attivi DBG
        this.maxUnacknowledgedBatches = 10;

        if (DBG_CHUNK_SYNC) {
            LOGGER.info(
                "[ImmPtlDbg] ACK unacked={} max={} desiredCpt={} quota={}",
                this.unacknowledgedBatches,
                this.maxUnacknowledgedBatches,
                this.desiredChunksPerTick,
                this.batchQuota
            );
        }
    }
}
