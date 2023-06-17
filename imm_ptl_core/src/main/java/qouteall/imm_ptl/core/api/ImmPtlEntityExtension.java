package qouteall.imm_ptl.core.api;

import net.minecraft.world.entity.Entity;

public interface ImmPtlEntityExtension {
    
    /**
     * Other mods should be able to overreide this without depending on ImmPtl.
     * @param portal the portal entity.
     *               use type Entity to make other mods to be able to override this without depending on ImmPtl.
     * @return whether the entity can teleport through the ImmPtl portal.
     */
    default boolean imm_ptl_canTeleportThroughPortal(Entity portal) {
        return true;
    }
}
