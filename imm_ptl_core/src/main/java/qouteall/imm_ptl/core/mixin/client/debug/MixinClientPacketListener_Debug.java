package qouteall.imm_ptl.core.mixin.client.debug;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener_Debug {
//    @Inject(
//        method = "handleChunkBlocksUpdate",
//        at = @At("RETURN")
//    )
//    private void onChunkBlockUpdate(
//        ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci
//    ) {
//        Helper.LOGGER.info("BlockUpdatePacket handle {}", RenderStates.frameIndex);
//    }
}
