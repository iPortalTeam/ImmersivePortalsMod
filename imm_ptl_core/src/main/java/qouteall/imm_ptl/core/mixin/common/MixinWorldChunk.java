package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.ducks.IEWorldChunk;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk implements IEWorldChunk {
}
