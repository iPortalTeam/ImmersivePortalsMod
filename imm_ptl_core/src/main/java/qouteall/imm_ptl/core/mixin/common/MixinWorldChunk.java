package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.ducks.IEWorldChunk;

@Mixin(LevelChunk.class)
public abstract class MixinWorldChunk implements IEWorldChunk {
}
