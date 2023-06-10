package qouteall.q_misc_util.my_util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.q_misc_util.Helper;

import java.util.function.Supplier;

// Log error and avoid spam.
public class LimitedLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(LimitedLogger.class);
    
    private int remain;
    
    public LimitedLogger(int maxCount) {
        remain = maxCount;
    }
    
    public void log(String s) {
        invoke(() -> Helper.log(s));
    }
    
    public void err(String s) {
        invoke(() -> Helper.err(s));
    }
    
    public void lInfo(Logger logger, String template, Object... args) {
        invoke(() -> logger.info(template, args));
    }
    
    public void lErr(Logger logger, String template, Object... args) {
        invoke(() -> logger.error(template, args));
    }
    
    public void lInfo(org.apache.logging.log4j.Logger logger, String template, Object... args) {
        invoke(() -> logger.info(template, args));
    }
    
    public void lErr(org.apache.logging.log4j.Logger logger, String template, Object... args) {
        invoke(() -> logger.error(template, args));
    }
    
    public void invoke(Runnable r) {
        if (remain > 0) {
            remain--;
            r.run();
            if (remain == 0) {
                LOGGER.info("The logging reached its limit. Similar log won't be displayed.");
            }
        }
    }
    
    public void throwException(Supplier<RuntimeException> s) {
        invoke(() -> {
            throw s.get();
        });
    }
}
