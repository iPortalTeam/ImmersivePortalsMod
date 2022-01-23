package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinServerPlayerEntity {
    @Inject(
        method = "Lnet/minecraft/server/level/ServerPlayer;untrackChunk(Lnet/minecraft/world/level/ChunkPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ci.cancel();
    }
}
