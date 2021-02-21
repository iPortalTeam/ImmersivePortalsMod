package com.qouteall.immersive_portals;

import net.minecraft.entity.Entity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

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
    
    public static Runnable resetViewport = invokeNothing;
    
    public static BiConsumer<RegistryKey<World>, RegistryKey<World>> onPlayerTraveled = (a, b) -> {
    };
    
    public static BooleanSupplier shouldDisableFog = returnFalse;
    
    public static Consumer<Object> createNewRenderInfosNormal = (a) -> {
    };
    
//    public static Runnable initShaderCullingManager = invokeNothing;
    
    public static BooleanSupplier isFogDisabled = returnFalse;
    
    public static Consumer<Entity> updateEntityTypeForShader = (a) -> {
    };
    
    public static BooleanSupplier isInternalShader = returnFalse;
}
