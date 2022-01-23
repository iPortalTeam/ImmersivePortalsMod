package qouteall.imm_ptl.core.mixin.common.entity_sync;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEEntityTracker;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.network.IPCommonNetwork;

import java.util.List;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage_E implements IEThreadedAnvilChunkStorage {
    
    @Shadow
    @Final
    public Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
    
    @Shadow
    abstract void updatePlayerStatus(ServerPlayer player, boolean added);
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Inject(
        method = "Lnet/minecraft/server/level/ChunkMap;removeEntity(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        //when the player leave this dimension, do not stop tracking entities
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            if (IPGlobal.serverTeleportationManager.isTeleporting(player)) {
                Object tracker = entityMap.remove(entity.getId());
                ((IEEntityTracker) tracker).stopTrackingToAllPlayers_();
                updatePlayerStatus(player, false);
                ci.cancel();
            }
        }
    }
    
    // Managed by EntitySync
    @Inject(method = "Lnet/minecraft/server/level/ChunkMap;tick()V", at = @At("HEAD"), cancellable = true)
    private void onTickEntityMovement(CallbackInfo ci) {
        ci.cancel();
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayer oldPlayer) {
        entityMap.values().forEach(obj -> {
            ((IEEntityTracker) obj).onPlayerRespawn(oldPlayer);
        });
    }
    
    /**
     * @link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)
     */
    @Override
    public void updateEntityTrackersAfterSendingChunkPacket(
        LevelChunk chunk, ServerPlayer player
    ) {
        List<Entity> attachedEntityList = Lists.newArrayList();
        List<Entity> passengerList = Lists.newArrayList();
        
        for (Object entityTracker : this.entityMap.values()) {
            Entity entity = ((IEEntityTracker) entityTracker).getEntity_();
            if (entity != player && entity.chunkPosition().equals(chunk.getPos())) {
                ((IEEntityTracker) entityTracker).updateEntityTrackingStatus(player);
                if (entity instanceof Mob && ((Mob) entity).getLeashHolder() != null) {
                    attachedEntityList.add(entity);
                }
                
                if (!entity.getPassengers().isEmpty()) {
                    passengerList.add(entity);
                }
            }
        }
        
        IPCommonNetwork.withForceRedirect(
            level,
            () -> {
                for (Entity entity : attachedEntityList) {
                    player.connection.send(new ClientboundSetEntityLinkPacket(
                        entity, ((Mob) entity).getLeashHolder()
                    ));
                }
                
                for (Entity entity : passengerList) {
                    player.connection.send(new ClientboundSetPassengersPacket(entity));
                }
            }
        );
    }
    
    @Override
    public void resendSpawnPacketToTrackers(Entity entity) {
        Object tracker = entityMap.get(entity.getId());
        Validate.notNull(tracker, "entity not yet tracked");
        ((IEEntityTracker) tracker).resendSpawnPacketToTrackers();
    }
    
    @Override
    public Int2ObjectMap<ChunkMap.TrackedEntity> getEntityTrackerMap() {
        return entityMap;
    }
}
