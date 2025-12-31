package qouteall.imm_ptl.core.mixin.client.accessor;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public interface IEClientChunkCacheStorage {
    @Invoker("inRange")
    boolean ip_inRange(int x, int z);
    
    @Invoker("getIndex")
    int ip_getIndex(int x, int z);
    
    @Invoker("getChunk")
    LevelChunk ip_getChunk(int index);
    
    @Invoker("replace")
    void ip_replace(int index, LevelChunk chunk);
    
    @Invoker("refreshEmptySections")
    void ip_refreshEmptySections(LevelChunk chunk);
    
    @Invoker("drop")
    void ip_drop(int index, LevelChunk chunk);
}
