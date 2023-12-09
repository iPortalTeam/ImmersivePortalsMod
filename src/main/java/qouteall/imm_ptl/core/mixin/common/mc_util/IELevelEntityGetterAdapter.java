package qouteall.imm_ptl.core.mixin.common.mc_util;

import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelEntityGetterAdapter.class)
public interface IELevelEntityGetterAdapter {
    @Accessor("sectionStorage")
    EntitySectionStorage<?> getCache();
    
    @Accessor("visibleEntities")
    EntityLookup<?> getIndex();
}
