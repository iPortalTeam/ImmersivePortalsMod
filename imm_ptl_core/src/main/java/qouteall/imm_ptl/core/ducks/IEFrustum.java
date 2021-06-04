package qouteall.imm_ptl.core.ducks;

public interface IEFrustum {
    boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
