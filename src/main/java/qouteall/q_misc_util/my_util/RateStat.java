package qouteall.q_misc_util.my_util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.DoubleConsumer;

public class RateStat {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final DoubleConsumer rateConsumer;
    private long lastUpdateSecond = 0;
    private int hitCount = 0;
    
    public RateStat(DoubleConsumer rateConsumer) {
        this.rateConsumer = rateConsumer;
    }
    
    public RateStat(String name) {
        this((rate) -> {
            LOGGER.info("{} rate: {}", name, rate);
        });
    }
    
    public void update() {
        long currTime = System.nanoTime();
    
        long currUpdateSecond = currTime / 1000000000;
    
        if (lastUpdateSecond == 0) {
            lastUpdateSecond = currUpdateSecond;
            return;
        }
    
        if (lastUpdateSecond != currUpdateSecond) {
            long passedSeconds = currUpdateSecond - lastUpdateSecond;
            int accumulatedHitCount = hitCount;
            hitCount = 0;
            lastUpdateSecond = currUpdateSecond;
            double rate = (double) accumulatedHitCount / passedSeconds;
            if (rate != 0.0) {
                rateConsumer.accept(rate);
            }
        }
    }
    
    public void hit() {
        update();
        hitCount++;
    }
}
