package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import com.qouteall.immersive_portals.network.CommonNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.network.Packet;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements IEServerWorld {
    
    @Shadow
    public abstract PersistentStateManager getPersistentStateManager();
    
    @Shadow
    public abstract ServerChunkManager getChunkManager();
    
    @Shadow
    @Final
    private ServerWorldProperties worldProperties;
    
    @Shadow
    @Final
    private Int2ObjectMap<Entity> entitiesById;
    
    @Shadow
    @Final
    private Map<UUID, Entity> entitiesByUuid;
    
    @Shadow
    @Final
    private Queue<Entity> entitiesToLoad;
    
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Mutable
    @Shadow @Final private ServerChunkManager serverChunkManager;
    
    @Shadow @Final private Set<EntityNavigation> entityNavigations;
    
    //in vanilla if a dimension has no player and no forced chunks then it will not tick
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    private boolean redirectIsEmpty(List list) {
        final ServerWorld this_ = (ServerWorld) (Object) this;
        if (NewChunkTrackingGraph.shouldLoadDimension(this_.getRegistryKey())) {
            return false;
        }
        return list.isEmpty();
    }
    
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/Packet;)V"
        ),
        require = 0 //Forge changes that. avoid crashing in forge version
    )
    private void redirectSendToAll(PlayerManager playerManager, Packet<?> packet) {
        final ServerWorld this_ = (ServerWorld) (Object) this;
        CommonNetwork.withForceRedirect(
            this_,
            () -> {
                playerManager.sendToAll(packet);
            }
        );
    }
    
    // for debug
    @Inject(method = "toString", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        final ServerWorld this_ = (ServerWorld) (Object) this;
        cir.setReturnValue("ServerWorld " + this_.getRegistryKey().getValue() +
            " " + worldProperties.getLevelName());
    }
    
    @Override
    public void debugDispose() {
        for (Entity e : entitiesById.values()) {
            e.world = null;
        }
        serverChunkManager=null;
        
        entitiesById.clear();
        entitiesByUuid.clear();
        entitiesToLoad.clear();
        players.clear();
        entityNavigations.clear();
    }
}
