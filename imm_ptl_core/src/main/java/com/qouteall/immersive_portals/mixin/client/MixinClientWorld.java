package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    @Mutable
    private ClientPlayNetworkHandler netHandler;
    
    @Mutable
    @Shadow
    @Final
    private ClientChunkManager chunkManager;
    
    @Shadow
    public abstract Entity getEntityById(int id);
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    @Mutable
    @Shadow
    @Final
    private WorldRenderer worldRenderer;
    private List<Portal> globalTrackedPortals;
    
    @Override
    public ClientPlayNetworkHandler getNetHandler() {
        return netHandler;
    }
    
    @Override
    public void setNetHandler(ClientPlayNetworkHandler handler) {
        netHandler = handler;
    }
    
    @Override
    public List<Portal> getGlobalPortals() {
        return globalTrackedPortals;
    }
    
    @Override
    public void setGlobalPortals(List<Portal> arg) {
        globalTrackedPortals = arg;
    }
    
    //use my client chunk manager
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPlayNetworkHandler clientPlayNetworkHandler, ClientWorld.Properties properties,
        RegistryKey<World> registryKey, DimensionType dimensionType, int i,
        Supplier<Profiler> supplier, WorldRenderer worldRenderer, boolean bl,
        long l, CallbackInfo ci
    ) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        ClientChunkManager myClientChunkManager =
            O_O.createMyClientChunkManager(clientWorld, i);
        chunkManager = myClientChunkManager;
    }
    
    // avoid entity duplicate when an entity travels
    @Inject(
        method = "addEntityPrivate",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientWorld world : ClientWorldLoader.getClientWorlds()) {
                if (world != (Object) this) {
                    world.removeEntity(entityId);
                }
            }
        }
    }
    
    /**
     * If the player goes into a portal when the other side chunk is not yet loaded
     * freeze the player so the player won't drop
     * {@link ClientPlayerEntity#tick()}
     */
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;isChunkLoaded(II)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsChunkLoaded(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        WorldChunk chunk = chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof EmptyChunk) {
            cir.setReturnValue(false);
//            Helper.log("chunk not loaded");
//            new Throwable().printStackTrace();
        }
    }
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    /**
     * @author qouteall
     * @reason vanilla logic may be wrong
     */
    @Overwrite
    private void checkEntityChunkPos(Entity entity) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        if (entity.isChunkPosUpdateRequested()) {
            this_.getProfiler().push("chunkCheck");
            int newCX = MathHelper.floor(entity.getX() / 16.0D);
            int newCY = MathHelper.floor(entity.getY() / 16.0D);
            int newCZ = MathHelper.floor(entity.getZ() / 16.0D);
            if (!entity.updateNeeded || entity.chunkX != newCX || entity.chunkY != newCY || entity.chunkZ != newCZ) {
                if (entity.updateNeeded && this_.isChunkLoaded(entity.chunkX, entity.chunkZ)) {
                    this_.getChunk(entity.chunkX, entity.chunkZ).remove(entity, entity.chunkY);
                }
                
                if (!entity.teleportRequested() && !this_.isChunkLoaded(newCX, newCZ)) {
                    if (entity.updateNeeded) {
                        limitedLogger.log("Entity left loaded chunk area " + entity);
                    }
                }
                else {
                    this_.getChunk(newCX, newCZ).addEntity(entity);
                }
            }
            
            this_.getProfiler().pop();
        }
    }
    
    // for debug
    @Inject(method = "toString", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        cir.setReturnValue("ClientWorld " + this_.getRegistryKey().getValue());
    }
    
    @Override
    public void resetWorldRendererRef() {
        worldRenderer = null;
    }
}
