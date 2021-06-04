package qouteall.imm_ptl.core.render.context_management;

import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ducks.IECamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

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
            copyContextFromObject, copyContextToObject, false,
            FogRendererContext::new
        );
        
        
    }
    
    public static void update() {
        swappingManager.setOuterDimension(RenderStates.originalPlayerDimension);
        swappingManager.resetChecks();
        if (ClientWorldLoader.getIsInitialized()) {
            ClientWorldLoader.getClientWorlds().forEach(world -> {
                RegistryKey<World> dimension = world.getRegistryKey();
                swappingManager.contextMap.computeIfAbsent(
                    dimension,
                    k -> new StaticFieldsSwappingManager.ContextRecord<>(
                        dimension,
                        new FogRendererContext(),
                        dimension != RenderStates.originalPlayerDimension
                    )
                );
            });
        }
    }
    
    public static Vec3d getFogColorOf(
        ClientWorld destWorld, Vec3d pos
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        client.getProfiler().push("get_fog_color");
        
        ClientWorld oldWorld = client.world;
        
        RegistryKey<World> newWorldKey = destWorld.getRegistryKey();
        
        swappingManager.contextMap.computeIfAbsent(
            newWorldKey,
            k -> new StaticFieldsSwappingManager.ContextRecord<>(
                k, new FogRendererContext(), true
            )
        );
        
        swappingManager.pushSwapping(newWorldKey);
        client.world = destWorld;
        
        Camera newCamera = new Camera();
        ((IECamera) newCamera).portal_setPos(pos);
        ((IECamera) newCamera).portal_setFocusedEntity(client.cameraEntity);
        
        try {
            BackgroundRenderer.render(
                newCamera,
                RenderStates.tickDelta,
                destWorld,
                client.options.viewDistance,
                client.gameRenderer.getSkyDarkness(RenderStates.tickDelta)
            );
            
            Vec3d result = getCurrentFogColor.get();
            
            return result;
        }
        finally {
            swappingManager.popSwapping();
            client.world = oldWorld;
            
            client.getProfiler().pop();
        }
    }
    
    public static void onPlayerTeleport(RegistryKey<World> from, RegistryKey<World> to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }
    
}
