package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import qouteall.imm_ptl.core.ducks.IEEntityLevelSetter;

@Mixin(Entity.class)
public interface MixinEntityLevelSetter extends IEEntityLevelSetter {
    @Invoker("setLevel")
    void ip_setLevel(Level level);
}
