package qouteall.imm_ptl.core.debug;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class DebugUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    
//    public static int acquireCounter = 0;
//    public static int releaseCounter = 0;
//
//    public static final AtomicInteger actualAcquire = new AtomicInteger();
    
    public static void init() {
//        IPGlobal.postServerTickSignal.connect(() -> {
//            int ac = actualAcquire.getAndSet(0);
//            if (acquireCounter != 0 || releaseCounter != 0 || ac!=0) {
//                LOGGER.info("Acquire {} ActualAcquire {} Release {}", acquireCounter, ac, releaseCounter);
//                acquireCounter = 0;
//                releaseCounter = 0;
//            }
//
//        });
    }
}
