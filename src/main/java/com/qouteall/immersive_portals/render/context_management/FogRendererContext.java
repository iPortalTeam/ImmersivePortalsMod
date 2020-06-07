package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FogRendererContext {
    public float red;
    public float green;
    public float blue;
    public int waterFogColor = -1;
    public int nextWaterFogColor = -1;
    public long lastWaterFogColorUpdateTime = -1L;
    
    public static Consumer<FogRendererContext> copyContextFromObject;
    public static Consumer<FogRendererContext> copyContextToObject;
    public static Supplier<Vec3d> getCurrentFogColor;
    
    public static StaticFieldsSwappingManager<FogRendererContext> swappingManager;
    
    public static void init() {
        //load the class and apply mixin
        BackgroundRenderer.class.hashCode();
        
        swappingManager = new StaticFieldsSwappingManager<>(
            copyContextFromObject, copyContextToObject
        );
        
    }
    
    public static void update() {
        swappingManager.setOuterDimension(RenderStates.originalPlayerDimension);
        swappingManager.resetChecks();
        CGlobal.clientWorldLoader.clientWorldMap.keySet().forEach(dimension ->
            swappingManager.contextMap.computeIfAbsent(
                dimension,
                k -> new StaticFieldsSwappingManager.ContextRecord<>(
                    dimension,
                    new FogRendererContext(),
                    dimension != RenderStates.originalPlayerDimension
                )
            )
        );
    }
    
    public static void onPlayerTeleport(RegistryKey<World> from, RegistryKey<World> to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }
    
}
