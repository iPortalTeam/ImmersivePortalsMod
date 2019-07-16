package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public class MixinThreadedAnvilChunkStorage implements IEThreadedAnvilChunkStorage {
    @Shadow
    int watchDistance;
    
    @Shadow
    @Final
    private ServerLightingProvider serverLightingProvider;
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Override
    public int getWatchDistance() {
        return watchDistance;
    }
    
    @Override
    public ServerWorld getWorld() {
        return world;
    }
    
    @Override
    public ServerLightingProvider getLightingProvider() {
        return serverLightingProvider;
    }
    
    @Inject(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;loadEntity(Lnet/minecraft/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void onEntityLoad(Entity entity_1, CallbackInfo ci) {
        Globals.entityTracker.onEntityLoad(world.dimension.getType(), entity_1);
    }
    
    @Inject(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;unloadEntity(Lnet/minecraft/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void onEntityUnload(Entity entity_1, CallbackInfo ci) {
        Globals.entityTracker.onEntityUnload(entity_1);
    }
}
