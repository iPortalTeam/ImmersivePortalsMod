package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.ducks.IEServerEntityManager;

@Mixin(PersistentEntitySectionManager.class)
public class MixinServerEntityManager implements IEServerEntityManager {
}
