package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WorldBorder.class)
public interface IEWorldBorder {
    @Accessor("listeners")
    List<BorderChangeListener> ip_getListeners();
}
