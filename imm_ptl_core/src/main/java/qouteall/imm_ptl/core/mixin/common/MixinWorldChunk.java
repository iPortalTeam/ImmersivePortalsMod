package qouteall.imm_ptl.core.mixin.common;

import qouteall.imm_ptl.core.ducks.IEWorldChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk implements IEWorldChunk {
}
