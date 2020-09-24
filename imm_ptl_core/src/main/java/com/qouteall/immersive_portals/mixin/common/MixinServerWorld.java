package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements IEServerWorld {
    
    @Shadow
    public abstract PersistentStateManager getPersistentStateManager();
    
    @Shadow
    public abstract ServerChunkManager getChunkManager();
    
    private static LongSortedSet dummy;
    
    static {
        dummy = new LongLinkedOpenHashSet();
        dummy.add(23333);
    }
    
    //in vanilla if a dimension has no player and no forced chunks then it will not tick
//    @Redirect(
//        method = "tick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/world/ServerWorld;getForcedChunks()Lit/unimi/dsi/fastutil/longs/LongSet;"
//        )
//    )
//    private LongSet redirectGetForcedChunks(ServerWorld world) {
//        if (NewChunkTrackingGraph.shouldLoadDimension(world.getRegistryKey())) {
//            return dummy;
//        }
//        else {
//            return world.getForcedChunks();
//        }
//    }
    
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
            return true;
        }
        return list.isEmpty();
    }
}
