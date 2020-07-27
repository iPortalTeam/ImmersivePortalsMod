package com.qouteall.immersive_portals.my_util;

import com.qouteall.immersive_portals.Helper;

// Log error and avoid spam
public class LimitedLogger {
    private int remain;
    
    public LimitedLogger(int maxCount) {
        remain = maxCount;
    }
    
    public void err(String s) {
        if (remain > 0) {
            remain--;
            Helper.err(s);
        }
    }
}
