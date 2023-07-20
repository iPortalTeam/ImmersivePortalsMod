package qouteall.imm_ptl.core.mixin.common.mc_util;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEEntityTrackingSection;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;

import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.function.Function;

@Mixin(EntitySection.class)
public class MixinEntitySection<T extends EntityAccess> implements IEEntityTrackingSection<T> {
    @Shadow
    @Final
    private ClassInstanceMultiMap<T> storage;
    
    /**
     * {@link EntitySection#getEntities(EntityTypeTest, AABB, AbortableIterationConsumer)}
     */
    @IPVanillaCopy
    @Override
    @Nullable
    public <Sub extends T, R> R ip_traverse(EntityTypeTest<T, Sub> type, Function<Sub, R> func) {
        Class<? extends T> baseClass = type.getBaseClass();
        Collection<? extends T> collection = this.storage.find(baseClass);
        if (collection.isEmpty()) {
            return null;
        }
        for (T entity1 : collection) {
            Sub entity2 = type.tryCast(entity1);
            if (entity2 != null) {
                R result = func.apply(entity2);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
