package qouteall.imm_ptl.core.chunk_loading;

public enum PerformanceLevel {
    good, medium, bad;
    
    public static PerformanceLevel getPerformanceLevel(
        int averageFPS,
        int averageFreeMemoryMB
    ) {
        if (averageFPS > 50 && averageFreeMemoryMB > 1000) {
            return good;
        }
        else if (averageFPS > 30 && averageFreeMemoryMB > 400) {
            return medium;
        }
        else {
            return bad;
        }
    }
    
    public static int getVisiblePortalRangeChunks(PerformanceLevel level) {
        if (level == good) {
            return 8;
        }
        else if (level == medium) {
            return 3;
        }
        else {
            return 1;
        }
    }
    
    public static int getIndirectVisiblePortalRangeChunks(PerformanceLevel level) {
        if (level == good) {
            return 2;
        }
        else if (level == medium) {
            return 1;
        }
        else {
            return 0;
        }
    }
    
    public static int getIndirectLoadingRadiusCap(
        PerformanceLevel level
    ) {
        if (level == good) {
            return 32;
        }
        else if (level == medium) {
            return 7;
        }
        else {
            return 2;
        }
    }
    
    public static int getPortalRenderingChunkRadiusCap(
        PerformanceLevel level
    ) {
        if (level == good) {
            return 9999;
        }
        else if (level == medium) {
            return 6;
        }
        else {
            return 2;
        }
    }
}
