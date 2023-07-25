package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEChunkTicketManager;

@Mixin(DistanceManager.class)
public abstract class MixinDistanceManager implements IEChunkTicketManager {
    
    @Shadow
    @Final
    private Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk;
    
    @Shadow
    protected abstract SortedArraySet<Ticket<?>> getTickets(long position);
    
    // avoid NPE
    @Inject(method = "Lnet/minecraft/server/level/DistanceManager;removePlayer(Lnet/minecraft/core/SectionPos;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void onHandleChunkLeave(
        SectionPos sectionPos,
        ServerPlayer serverPlayer,
        CallbackInfo ci
    ) {
        long chunkPos = sectionPos.chunk().toLong();
        playersPerChunk.computeIfAbsent(chunkPos, k -> new ObjectOpenHashSet<>());
    }
    
    @Override
    public SortedArraySet<Ticket<?>> portal_getTicketSet(long chunkPos) {
        return getTickets(chunkPos);
    }
}
