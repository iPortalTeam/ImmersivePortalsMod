package qouteall.imm_ptl.core.mixin.common.entity_sync;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEEntityTracker;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.network.IPCommonNetwork;

import java.util.List;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage_E implements IEThreadedAnvilChunkStorage {
    
    @Shadow
    @Final
    public Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers;
    
    @Shadow
    abstract void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added);
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Inject(
        method = "unloadEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        //when the player leave this dimension, do not stop tracking entities
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (IPGlobal.serverTeleportationManager.isTeleporting(player)) {
                Object tracker = entityTrackers.remove(entity.getId());
                ((IEEntityTracker) tracker).stopTrackingToAllPlayers_();
                handlePlayerAddedOrRemoved(player, false);
                ci.cancel();
            }
        }
    }
    
    // Managed by EntitySync
    @Inject(method = "tickEntityMovement", at = @At("HEAD"), cancellable = true)
    private void onTickEntityMovement(CallbackInfo ci) {
        ci.cancel();
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        entityTrackers.values().forEach(obj -> {
            ((IEEntityTracker) obj).onPlayerRespawn(oldPlayer);
        });
    }
    
    /**
     * @link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)
     */
    @Override
    public void updateEntityTrackersAfterSendingChunkPacket(
        WorldChunk chunk, ServerPlayerEntity player
    ) {
        List<Entity> attachedEntityList = Lists.newArrayList();
        List<Entity> passengerList = Lists.newArrayList();
        
        for (Object entityTracker : this.entityTrackers.values()) {
            Entity entity = ((IEEntityTracker) entityTracker).getEntity_();
            if (entity != player && entity.getChunkPos().equals(chunk.getPos())) {
                ((IEEntityTracker) entityTracker).updateEntityTrackingStatus(player);
                if (entity instanceof MobEntity && ((MobEntity) entity).getHoldingEntity() != null) {
                    attachedEntityList.add(entity);
                }
                
                if (!entity.getPassengerList().isEmpty()) {
                    passengerList.add(entity);
                }
            }
        }
        
        IPCommonNetwork.withForceRedirect(
            world,
            () -> {
                for (Entity entity : attachedEntityList) {
                    player.networkHandler.sendPacket(new EntityAttachS2CPacket(
                        entity, ((MobEntity) entity).getHoldingEntity()
                    ));
                }
                
                for (Entity entity : passengerList) {
                    player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
                }
            }
        );
    }
    
    /**
     * @author qouteall
     * @reason make mod incompat fail fast
     * Will be managed by {@link qouteall.imm_ptl.core.chunk_loading.ServerEntityStorageManagement}
     */
    @Overwrite
    public void onChunkStatusChange(ChunkPos chunkPos, ChunkHolder.LevelType levelType) {
        // nothing
    }
    
    @Override
    public void resendSpawnPacketToTrackers(Entity entity) {
        Object tracker = entityTrackers.get(entity.getId());
        Validate.notNull(tracker, "entity not yet tracked");
        ((IEEntityTracker) tracker).resendSpawnPacketToTrackers();
    }
    
    @Override
    public Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> getEntityTrackerMap() {
        return entityTrackers;
    }
}
