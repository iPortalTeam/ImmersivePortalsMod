package qouteall.imm_ptl.core.mixin.common.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueOutput.class)
public interface IETagValueOutput {
    @Accessor("output")
    CompoundTag ip_getOutput();
}
