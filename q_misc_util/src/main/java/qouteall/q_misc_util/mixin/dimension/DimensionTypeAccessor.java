package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DimensionType.class)
public interface DimensionTypeAccessor {
    @Accessor("DEFAULT_NETHER")
    public static DimensionType _getTheNether() {
        throw new RuntimeException();
    }
    
    @Accessor("DEFAULT_END")
    public static DimensionType _getTheEnd() {
        throw new RuntimeException();
    }
    
    
}
