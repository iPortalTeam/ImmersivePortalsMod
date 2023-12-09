package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

public interface IEServerWorld {
    PersistentEntitySectionManager<Entity> ip_getEntityManager();
}
