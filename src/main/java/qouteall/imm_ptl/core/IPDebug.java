package qouteall.imm_ptl.core;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class IPDebug {
    private static final Logger LOGGER = LogUtils.getLogger();

    // attiva con -Dimmptl.debug=true
    public static final boolean ENABLED = Boolean.getBoolean("immptl.debug");

    public static void log(String fmt, Object... args) {
        if (ENABLED) {
            LOGGER.info("[ImmPtlDbg] " + fmt, args);
        }
    }

    private IPDebug() {}
}
