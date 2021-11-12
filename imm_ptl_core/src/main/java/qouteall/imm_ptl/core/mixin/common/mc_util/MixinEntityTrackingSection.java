package qouteall.imm_ptl.core.mixin.common.mc_util;

import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEEntityTrackingSection;

import java.util.Collection;
import java.util.function.Consumer;

@Mixin(EntityTrackingSection.class)
public class MixinEntityTrackingSection implements IEEntityTrackingSection {
    @Shadow
    @Final
    private TypeFilterableList collection;
    
    @Override
    public void myForeach(TypeFilter type, Consumer action) {
        Collection collection = this.collection.getAllOfType(type.getBaseClass());
        if (collection.isEmpty()) {
            return;
        }
        for (Object entityLike : collection) {
            EntityLike entityLike2 = (EntityLike) type.downcast(entityLike);
            if (entityLike2 == null) continue;
            action.accept(entityLike2);
        }
    }
}
