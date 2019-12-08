package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.util.math.Vec3d;
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
        
        ModMain.preRenderSignal.connect(() -> {
            update();
        });
    }
    
    private static void update() {
        swappingManager.setOuterDimension(RenderHelper.originalPlayerDimension);
        swappingManager.resetChecks();
        DimensionType.getAll().forEach(dimension ->
            swappingManager.contextMap.computeIfAbsent(
                dimension,
                k -> new StaticFieldsSwappingManager.ContextRecord<>(
                    dimension,
                    new FogRendererContext(),
                    dimension != RenderHelper.originalPlayerDimension
                )
            )
        );
    }
    
    public static void onPlayerTeleport(DimensionType from, DimensionType to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }
    
}
