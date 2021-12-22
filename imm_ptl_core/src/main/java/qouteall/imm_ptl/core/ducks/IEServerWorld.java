package qouteall.imm_ptl.core.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;

public interface IEServerWorld {
    ServerEntityManager<Entity> ip_getEntityManager();
}
