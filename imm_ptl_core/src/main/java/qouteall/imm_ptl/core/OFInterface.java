package qouteall.imm_ptl.core;

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
    
    public static BooleanSupplier isShadowPass = returnFalse;
    
    public static Consumer<Object> createNewRenderInfosNormal = (a) -> {
    };
    
    
}
