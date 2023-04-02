package qouteall.q_misc_util.my_util;

public interface BoxPredicate {
    public static final BoxPredicate nonePredicate =
        (double minX, double minY, double minZ, double maxX, double maxY, double maxZ) -> false;
    
    boolean test(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
