package qouteall.imm_ptl.core.portal.global_portals;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.portal.Portal;

public class GlobalTrackedPortal extends Portal {
    public static EntityType<GlobalTrackedPortal> entityType;
    
    public GlobalTrackedPortal(
        EntityType<?> entityType_1,
        Level world_1
    ) {
        super(entityType_1, world_1);
    }
    
}
