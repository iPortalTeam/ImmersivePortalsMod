package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    @Mutable
    private ClientPacketListener connection;
    
    @Mutable
    @Shadow
    @Final
    private ClientChunkCache chunkSource;
    
    @Shadow
    public abstract Entity getEntity(int id);
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Mutable
    @Shadow
    @Final
    private LevelRenderer levelRenderer;
    
    @Shadow
    @Final
    private EntityTickList tickingEntities;
    private List<Portal> portal_globalPortals;
    
    @Override
    public ClientPacketListener getNetHandler() {
        return connection;
    }
    
    @Override
    public void setNetHandler(ClientPacketListener handler) {
        connection = handler;
    }
    
    @Override
    public List<Portal> getGlobalPortals() {
        return portal_globalPortals;
    }
    
    @Override
    public void setGlobalPortals(List<Portal> arg) {
        portal_globalPortals = arg;
    }
    
    //use my client chunk manager
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPacketListener netHandler, ClientLevel.ClientLevelData properties,
        ResourceKey<Level> registryRef, DimensionType dimensionType, int loadDistance,
        int simulationDistance, Supplier<ProfilerFiller> profiler, LevelRenderer worldRenderer,
        boolean debugWorld, long seed, CallbackInfo ci
    ) {
        ClientLevel clientWorld = (ClientLevel) (Object) this;
        ClientChunkCache myClientChunkManager =
            O_O.createMyClientChunkManager(clientWorld, loadDistance);
        chunkSource = myClientChunkManager;
    }
    
    // avoid entity duplicate when an entity travels
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/ClientLevel;addEntity(ILnet/minecraft/world/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientLevel world : ClientWorldLoader.getClientWorlds()) {
                if (world != (Object) this) {
                    world.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
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
        method = "Lnet/minecraft/client/multiplayer/ClientLevel;hasChunk(II)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsChunkLoaded(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        if (IPGlobal.tickOnlyIfChunkLoaded) {
            LevelChunk chunk = chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk == null || chunk instanceof EmptyLevelChunk) {
                cir.setReturnValue(false);
                //            Helper.log("chunk not loaded");
                //            new Throwable().printStackTrace();
            }
        }
    }
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    // for debug
    @Inject(method = "Lnet/minecraft/client/multiplayer/ClientLevel;toString()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        ClientLevel this_ = (ClientLevel) (Object) this;
        cir.setReturnValue("ClientWorld " + this_.dimension().location());
    }
    
    @Override
    public void resetWorldRendererRef() {
        levelRenderer = null;
    }
    
    @Override
    public EntityTickList ip_getEntityList() {
        return tickingEntities;
    }
}
