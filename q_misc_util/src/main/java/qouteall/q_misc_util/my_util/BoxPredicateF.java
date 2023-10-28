package qouteall.q_misc_util.my_util;

public interface BoxPredicateF {
    public static final BoxPredicateF nonePredicate =
        (float minX, float minY, float minZ, float maxX, float maxY, float maxZ) -> false;
    
    boolean test(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
