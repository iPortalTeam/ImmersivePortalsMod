package qouteall.imm_ptl.peripheral.mixin.common.fix_concurrency;

import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShufflingList.class)
public abstract class MixinWeightedList {
//    @Shadow
//    public abstract WeightedList shuffle(Random random);
//
//    // it's not thread safe
//    // dimension stack made this vanilla issue trigger more frequently
//    @Overwrite
//    public Object pickRandom(Random random) {
//        for (; ; ) {
//            try {
//                return this.shuffle(random).stream().findFirst().orElseThrow(RuntimeException::new);
//            }
//            catch (Throwable throwable) {
//                // including ConcurrentModificationException
//                throwable.printStackTrace();
//            }
//        }
//    }
}
