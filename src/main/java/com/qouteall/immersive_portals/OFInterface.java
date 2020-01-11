package com.qouteall.immersive_portals;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

//all calls to OptiFine-specific interfaces are done through functions here
public class OFInterface {
    private static final Runnable invokeNothing = () -> {
    };
    private static final BooleanSupplier returnFalse = () -> false;
    
    public static boolean isOptifinePresent = false;
    
    public static BooleanSupplier isShaders = returnFalse;
    
    public static BooleanSupplier isShadowPass = returnFalse;
    
    public static Runnable bindToShaderFrameBuffer = invokeNothing;
    
    public static FloatConsumer beforeRenderCenter = f -> {
    };
    
    public static Runnable afterRenderCenter = invokeNothing;
    
    public static Runnable resetViewport = invokeNothing;
    
    public static BiConsumer<DimensionType, DimensionType> onPlayerTraveled = (a, b) -> {
    };
    
    public static BooleanSupplier shouldDisableFog = returnFalse;
    
    public static Consumer<Object> createNewRenderInfosNormal = (a) -> {
    };
    
    public static Runnable initShaderCullingManager = invokeNothing;
    
}
