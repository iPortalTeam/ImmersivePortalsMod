package qouteall.imm_ptl.peripheral.wand;

import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;

public enum PortalCorner {
    LEFT_BOTTOM, LEFT_TOP, RIGHT_BOTTOM, RIGHT_TOP;
    
    public int getXSign() {
        return switch (this) {
            case LEFT_BOTTOM, LEFT_TOP -> -1;
            case RIGHT_BOTTOM, RIGHT_TOP -> 1;
        };
    }
    
    public int getYSign() {
        return switch (this) {
            case LEFT_BOTTOM, RIGHT_BOTTOM -> -1;
            case LEFT_TOP, RIGHT_TOP -> 1;
        };
    }
    
    public Vec3 getOffset(Portal portal) {
        return portal.axisW.scale((portal.width / 2) * getXSign())
            .add(portal.axisH.scale((portal.height / 2) * getYSign()));
    }
    
    public Vec3 getPos(Portal portal) {
        return portal.getOriginPos().add(getOffset(portal));
    }
}
