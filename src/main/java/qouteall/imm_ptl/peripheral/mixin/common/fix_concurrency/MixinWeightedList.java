package qouteall.imm_ptl.peripheral.mixin.common.fix_concurrency;

import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShufflingList.class)
public abstract class MixinWeightedList {

}
