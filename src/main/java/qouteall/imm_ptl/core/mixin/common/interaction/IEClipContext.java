package qouteall.imm_ptl.core.mixin.common.interaction;

import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClipContext.class)
public interface IEClipContext {
    @Accessor("block")
    ClipContext.Block ip_getBlock();
    
    @Accessor("fluid")
    ClipContext.Fluid ip_getFluid();
}
