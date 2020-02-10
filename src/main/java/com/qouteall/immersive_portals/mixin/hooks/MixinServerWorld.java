package com.qouteall.immersive_portals.mixin.hooks;

import com.google.common.collect.Lists;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ForcedChunkState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements IEServerWorld {
    
    @Shadow
    public abstract PersistentStateManager getPersistentStateManager();
    
    @Shadow
    public abstract ServerChunkManager getChunkManager();
    
    @Override
    public void setChunkForcedWithoutImmediateLoading(int x, int z, boolean forced) {
        ForcedChunkState forcedChunkState = (ForcedChunkState) this.getPersistentStateManager().getOrCreate(
            ForcedChunkState::new,
            "chunks"
        );
        ChunkPos chunkPos = new ChunkPos(x, z);
        long l = chunkPos.toLong();
        boolean bl2;
        if (forced) {
            bl2 = forcedChunkState.getChunks().add(l);
        }
        else {
            bl2 = forcedChunkState.getChunks().remove(l);
        }
        
        forcedChunkState.setDirty(bl2);
        if (bl2) {
            this.getChunkManager().setChunkForced(chunkPos, forced);
        }
        
        
    }
    
    @Override
    public <T extends Entity> List<T> getEntitiesWithoutImmediateChunkLoading(
        Class<? extends T> entityClass, Box box, Predicate<? super T> predicate
    ) {
        int i = MathHelper.floor((box.x1 - 2.0D) / 16.0D);
        int j = MathHelper.ceil((box.x2 + 2.0D) / 16.0D);
        int k = MathHelper.floor((box.z1 - 2.0D) / 16.0D);
        int l = MathHelper.ceil((box.z2 + 2.0D) / 16.0D);
        List<T> list = Lists.newArrayList();
        ChunkManager chunkManager = this.getChunkManager();
        
        for (int m = i; m < j; ++m) {
            for (int n = k; n < l; ++n) {
                WorldChunk worldChunk = (WorldChunk) portal_getChunkIfLoaded(m, n);
                if (worldChunk != null) {
                    worldChunk.getEntities((Class) entityClass, box, list, predicate);
                }
            }
        }
        
        return list;
    }
    
    private Chunk portal_getChunkIfLoaded(
        int x, int z
    ) {
        ThreadedAnvilChunkStorage storage = getChunkManager().threadedAnvilChunkStorage;
        IEThreadedAnvilChunkStorage ieStorage = (IEThreadedAnvilChunkStorage) storage;
        ChunkHolder chunkHolder = ieStorage.getChunkHolder_(ChunkPos.toLong(x, z));
        if (chunkHolder != null) {
            WorldChunk chunk = chunkHolder.getWorldChunk();
            if (chunk != null) {
                return chunk;
            }
        }
        return null;
    }
}
