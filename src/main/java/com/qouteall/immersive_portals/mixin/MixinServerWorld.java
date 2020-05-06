package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;getForcedChunks()Lit/unimi/dsi/fastutil/longs/LongSet;"
        )
    )
    private LongSet redirectGetForcedChunks(ServerWorld world) {
        if (NewChunkTrackingGraph.shouldLoadDimension(world.dimension.getType())) {
            return dummy;
        }
        else {
            return world.getForcedChunks();
        }
    }
    
//    @Override
//    public <T extends Entity> List<T> getEntitiesWithoutImmediateChunkLoading(
//        Class<? extends T> entityClass, Box box, Predicate<? super T> predicate
//    ) {
//        int i = MathHelper.floor((box.x1 - 2.0D) / 16.0D);
//        int j = MathHelper.ceil((box.x2 + 2.0D) / 16.0D);
//        int k = MathHelper.floor((box.z1 - 2.0D) / 16.0D);
//        int l = MathHelper.ceil((box.z2 + 2.0D) / 16.0D);
//        List<T> list = Lists.newArrayList();
//        ChunkManager chunkManager = this.getChunkManager();
//
//        for (int m = i; m < j; ++m) {
//            for (int n = k; n < l; ++n) {
//                WorldChunk worldChunk = (WorldChunk) portal_getChunkIfLoaded(m, n);
//                if (worldChunk != null) {
//                    worldChunk.getEntities((Class) entityClass, box, list, predicate);
//                }
//            }
//        }
//
//        return list;
//    }
//
//    private Chunk portal_getChunkIfLoaded(
//        int x, int z
//    ) {
//        ThreadedAnvilChunkStorage storage = getChunkManager().threadedAnvilChunkStorage;
//        IEThreadedAnvilChunkStorage ieStorage = (IEThreadedAnvilChunkStorage) storage;
//        ChunkHolder chunkHolder = ieStorage.getChunkHolder_(ChunkPos.toLong(x, z));
//        if (chunkHolder != null) {
//            WorldChunk chunk = chunkHolder.getWorldChunk();
//            if (chunk != null) {
//                return chunk;
//            }
//        }
//        return null;
//    }
}
