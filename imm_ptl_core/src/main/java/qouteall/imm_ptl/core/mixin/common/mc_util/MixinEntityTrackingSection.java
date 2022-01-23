package qouteall.imm_ptl.core.mixin.common.mc_util;

import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEEntityTrackingSection;

import java.util.Collection;
import java.util.function.Consumer;

@Mixin(EntitySection.class)
public class MixinEntityTrackingSection implements IEEntityTrackingSection {
    @Shadow
    @Final
    private ClassInstanceMultiMap storage;
    
    @Override
    public void myForeach(EntityTypeTest type, Consumer action) {
        Collection collection = this.storage.find(type.getBaseClass());
        if (collection.isEmpty()) {
            return;
        }
        for (Object entityLike : collection) {
            EntityAccess entityLike2 = (EntityAccess) type.tryCast(entityLike);
            if (entityLike2 == null) continue;
            action.accept(entityLike2);
        }
    }
}
