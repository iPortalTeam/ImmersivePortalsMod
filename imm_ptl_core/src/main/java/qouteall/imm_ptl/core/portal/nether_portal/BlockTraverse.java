package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.util.math.BlockPos;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.function.Function;
import java.util.function.Predicate;

public class BlockTraverse {
    
    public static interface IntFunc<T> {
        T eval(int i);
    }
    
    public static interface BiIntFunc<T> {
        T eval(int x, int z);
    }
    
    public static interface TriIntFunc<T> {
        T eval(int x, int y, int z);
    }
    
    // the from and to are inclusive!
    public static <T> T searchFromTo(int from, int to, IntFunc<T> func) {
        if (from > to) {
            for (int i = from; i >= to; i--) {
                T obj = func.eval(i);
                if (obj != null) {
                    return obj;
                }
            }
        }
        else {
            for (int i = from; i <= to; i++) {
                T obj = func.eval(i);
                if (obj != null) {
                    return obj;
                }
            }
        }
        return null;
    }
    
    public static <T> T searchOnPlane(
        int centerX, int centerZ, int range,
        BiIntFunc<T> func
    ) {
        T centerResult = func.eval(centerX, centerZ);
        if (centerResult != null) {
            return centerResult;
        }
        
        for (int layer = 1; layer < range; layer++) {
            for (int w = 0; w < layer * 2; w++) {
                T obj = func.eval(layer + centerX, w + 1 - layer + centerZ);
                if (obj != null) {
                    return obj;
                }
            }
            
            for (int w = 0; w < layer * 2; w++) {
                T obj = func.eval(-w + layer - 1 + centerX, layer + centerZ);
                if (obj != null) {
                    return obj;
                }
            }
            
            for (int w = 0; w < layer * 2; w++) {
                T obj = func.eval(-layer + centerX, -w + layer - 1 + centerZ);
                if (obj != null) {
                    return obj;
                }
            }
            
            for (int w = 0; w < layer * 2; w++) {
                T obj = func.eval(w + 1 - layer + centerX, -layer + centerZ);
                if (obj != null) {
                    return obj;
                }
            }
        }
        
        return null;
    }
    
    public static <T> T searchColumnedRaw(
        int centerX, int centerZ, int range,
        int startY, int endY,
        TriIntFunc<T> func
    ) {
        return searchOnPlane(
            centerX, centerZ, range,
            (x, z) -> searchFromTo(startY, endY, y -> func.eval(x, y, z))
        );
    }
    
    // NOTE Mutable block pos
    // NOTE the startY and endY are inclusive
    public static <T> T searchColumned(
        int centerX, int centerZ, int range,
        int startY, int endY,
        Function<BlockPos, T> func
    ) {
        BlockPos.Mutable temp = new BlockPos.Mutable();
        return searchColumnedRaw(
            centerX, centerZ, range, startY, endY,
            (x, y, z) -> {
                temp.set(x, y, z);
                return func.apply(temp);
            }
        );
    }
    
    public static <T> T searchInBox(IntBox box, TriIntFunc<T> func) {
        for (int x = box.l.getX(); x <= box.h.getX(); x++) {
            for (int y = box.l.getY(); y <= box.h.getY(); y++) {
                for (int z = box.l.getZ(); z <= box.h.getZ(); z++) {
                    final T obj = func.eval(x, y, z);
                    if (obj != null) {
                        return obj;
                    }
                }
            }
        }
        return null;
    }
    
    // NOTE Mutable block pos
    public static <T> T searchInBox(IntBox box, Function<BlockPos, T> func) {
        BlockPos.Mutable temp = new BlockPos.Mutable();
        return searchInBox(box,
            (x, y, z) -> {
                temp.set(x, y, z);
                return func.apply(temp);
            }
        );
    }
    
    // NOTE Mutable block pos
    public static boolean boxAllMatch(IntBox box, Predicate<BlockPos> predicate) {
        Boolean result = searchInBox(box, mutable -> {
            if (predicate.test(mutable)) {
                return Boolean.valueOf(true);
            }
            return null;
        });
        return result != null;
    }
}
