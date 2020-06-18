package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IECamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
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
    
    public static Vec3d getFogColorOf(
        ClientWorld world, Vec3d pos
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        swappingManager.pushSwapping(world.getRegistryKey());
        
        Camera newCamera = new Camera();
        ((IECamera) newCamera).mySetPos(pos);
        
        BackgroundRenderer.render(
            newCamera,
            RenderStates.tickDelta,
            world,
            client.options.viewDistance,
            client.gameRenderer.getSkyDarkness(RenderStates.tickDelta)
        );
        
        Vec3d result = getCurrentFogColor.get();
        
        swappingManager.popSwapping();
        
        return result;
    }
    
    public static void onPlayerTeleport(RegistryKey<World> from, RegistryKey<World> to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }
    
}
