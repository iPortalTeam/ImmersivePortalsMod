package com.qouteall.immersive_portals.my_util;

import com.qouteall.immersive_portals.Helper;

import java.util.function.Supplier;

// Log error and avoid spam
// TODO use this to replace these
public class LimitedLogger {
    private int remain;
    
    public LimitedLogger(int maxCount) {
        remain = maxCount;
    }
    
    public void err(String s) {
        invoke(() -> Helper.err(s));
    }
    
    public void invoke(Runnable r) {
        if (remain > 0) {
            remain--;
            r.run();
        }
    }
    
    public void throwException(Supplier<RuntimeException> s) {
        invoke(() -> {
            throw s.get();
        });
    }
}
