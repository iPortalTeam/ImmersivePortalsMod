package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEServerWorld;

import java.util.List;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements IEServerWorld {
    
    @Shadow
    public abstract DimensionDataStorage getDataStorage();
    
    @Shadow
    public abstract ServerChunkCache getChunkSource();
    
    @Shadow
    @Final
    private ServerLevelData serverLevelData;
    
    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;
    
    //in vanilla if a dimension has no player and no forced chunks then it will not tick
    @Redirect(
        method = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    private boolean redirectIsEmpty(List list) {
        final ServerLevel this_ = (ServerLevel) (Object) this;
        if (NewChunkTrackingGraph.shouldLoadDimension(this_.dimension())) {
            return false;
        }
        return list.isEmpty();
    }
    
    // for debug
    @Inject(method = "Lnet/minecraft/server/level/ServerLevel;toString()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        final ServerLevel this_ = (ServerLevel) (Object) this;
        cir.setReturnValue("ServerWorld " + this_.dimension().location() +
            " " + serverLevelData.getLevelName());
    }
    
    @Inject(
        method = "tickNonPassenger",
        at = @At("HEAD")
    )
    private void onTickNonPassenger(Entity entity, CallbackInfo ci) {
        // this should be done right before setting last tick pos to this tick pos
        ((IEEntity) entity).ip_tickCollidingPortal();
    }
    
    @Override
    public PersistentEntitySectionManager<Entity> ip_getEntityManager() {
        return entityManager;
    }
}
