package com.qouteall.immersive_portals.optifine_compatibility;

import java.util.function.BooleanSupplier;

@Deprecated
public class OFInterface {
    public static final Runnable invokeNothing = () -> {
    };
    
    public static BooleanSupplier isOptifinePresent = () -> false;
    public static BooleanSupplier isShaders = () -> false;
    public static BooleanSupplier isShadowPass = () -> false;
    public static Runnable onBeginCreatingFakedWorld = invokeNothing;
    public static Runnable onFinishCreatingFakedWorld = invokeNothing;
}
