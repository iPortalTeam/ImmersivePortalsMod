package qouteall.imm_ptl.core.mixin.common.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueInput.class)
public interface IETagValueInput {
    @Accessor("input")
    CompoundTag ip_getInput();
}
