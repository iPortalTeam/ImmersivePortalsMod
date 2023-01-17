package qouteall.imm_ptl.core.mixin.client.sync;

import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStatePredictionHandler.class)
public interface IEBlockStatePredictionHandler {
    @Accessor("currentSequenceNr")
    void ip_setCurrentSequenceNumber(int arg);
}
