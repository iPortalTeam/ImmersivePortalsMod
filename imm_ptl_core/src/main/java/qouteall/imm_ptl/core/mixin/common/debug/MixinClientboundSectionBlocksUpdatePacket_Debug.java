package qouteall.imm_ptl.core.mixin.common.debug;

import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public class MixinClientboundSectionBlocksUpdatePacket_Debug {
//    @Inject(
//        method = "<init>(Lnet/minecraft/core/SectionPos;Lit/unimi/dsi/fastutil/shorts/ShortSet;Lnet/minecraft/world/level/chunk/LevelChunkSection;)V",
//        at = @At("RETURN")
//    )
//    private void onInit(SectionPos sectionPos, ShortSet positions, LevelChunkSection section, CallbackInfo ci) {
//        Helper.LOGGER.info("BlockUpdatePacket create {}", MiscHelper.getServer().overworld().getGameTime());
//    }
}
