package qouteall.imm_ptl.core.mixin.common.entity_sync;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkMap.TrackedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.chunk_loading.EntitySync;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IETrackedEntity;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;

import java.util.List;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap_E implements IEChunkMap {
    
    @Shadow
    @Final
    public Int2ObjectMap<TrackedEntity> entityMap;
    
    @Shadow
    abstract void updatePlayerStatus(ServerPlayer player, boolean added);
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Shadow
    protected abstract @Nullable ChunkHolder getUpdatingChunkIfPresent(long l);
    
    @Redirect(
        method = "addEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;updatePlayers(Ljava/util/List;)V"
        )
    )
    private void redirectUpdatePlayers(
        TrackedEntity trackedEntity, List<ServerPlayer> playersList
    ) {
        ((IETrackedEntity) trackedEntity).ip_updateEntityTrackingStatus();
    }
    
    @IPVanillaCopy
    @Inject(
        method = "Lnet/minecraft/server/level/ChunkMap;removeEntity(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        // when the player leave this dimension, do not stop tracking entities
        if (ServerTeleportationManager.of(entity.getServer()).isTeleporting(entity)) {
            if (entity instanceof ServerPlayer player) {
                Object tracker = entityMap.remove(entity.getId());
                ((IETrackedEntity) tracker).ip_stopTrackingToAllPlayers();
                updatePlayerStatus(player, false);
            }
            else {
                entityMap.remove(entity.getId());
            }
            
            ci.cancel();
        }
    }
    
    /**
     * Managed by {@link EntitySync}
     */
    @Inject(method = "Lnet/minecraft/server/level/ChunkMap;tick()V", at = @At("HEAD"), cancellable = true)
    private void onTickEntityMovement(CallbackInfo ci) {
        ci.cancel();
    }
    
    @Override
    public void ip_onPlayerUnload(ServerPlayer oldPlayer) {
        entityMap.values().forEach(obj -> {
            obj.removePlayer(oldPlayer);
        });
    }
    
    @Override
    public void ip_onDimensionRemove() {
        entityMap.values().forEach(obj -> {
            ((IETrackedEntity) obj).ip_onDimensionRemove();
        });
    }
    
    @Override
    public void ip_resendSpawnPacketToTrackers(Entity entity) {
        Object tracker = entityMap.get(entity.getId());
        Validate.notNull(tracker, "entity not yet tracked");
        ((IETrackedEntity) tracker).ip_resendSpawnPacketToTrackers();
    }
    
    @Override
    public Int2ObjectMap<TrackedEntity> ip_getEntityTrackerMap() {
        return entityMap;
    }
    
    @Override
    public @Nullable ChunkHolder ip_getUpdatingChunkIfPresent(long chunkPos) {
        return getUpdatingChunkIfPresent(chunkPos);
    }
}
