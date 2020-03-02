package com.qouteall.immersive_portals.mixin.entity_sync;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage_E implements IEThreadedAnvilChunkStorage {
    @Shadow
    @Final
    private Int2ObjectMap entityTrackers;
    
    @Shadow
    abstract void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added);
    
    @Inject(
        method = "unloadEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        //when the player leave this dimension, do not stop tracking entities
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (Global.serverTeleportationManager.isTeleporting(player)) {
                entityTrackers.remove(entity.getEntityId());
                handlePlayerAddedOrRemoved(player, false);
                ci.cancel();
            }
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        entityTrackers.values().forEach(obj -> {
            ((IEEntityTracker) obj).onPlayerRespawn(oldPlayer);
        });
    }
}
