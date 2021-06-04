package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import qouteall.imm_ptl.core.ducks.IEChunkTicketManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkTicketManager.class)
public abstract class MixinChunkTicketManager implements IEChunkTicketManager {
    
    @Shadow
    @Final
    private Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;
    
    
    @Shadow
    protected abstract void setWatchDistance(int viewDistance);
    
    @Shadow protected abstract SortedArraySet<ChunkTicket<?>> getTicketSet(long position);
    
    //avoid NPE
    @Inject(method = "handleChunkLeave", at = @At("HEAD"))
    private void onHandleChunkLeave(
        ChunkSectionPos chunkSectionPos_1,
        ServerPlayerEntity serverPlayerEntity_1,
        CallbackInfo ci
    ) {
        long long_1 = chunkSectionPos_1.toChunkPos().toLong();
        playersByChunkPos.putIfAbsent(long_1, new ObjectOpenHashSet<>());
    }
    
    @Override
    public void mySetWatchDistance(int newWatchDistance) {
        setWatchDistance(newWatchDistance);
    }
    
    @Override
    public SortedArraySet<ChunkTicket<?>> portal_getTicketSet(long chunkPos) {
        return getTicketSet(chunkPos);
    }
}
