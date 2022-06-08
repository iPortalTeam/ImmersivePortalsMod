package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ducks.IECamera;

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
    public static Supplier<Vec3> getCurrentFogColor;
    
    public static StaticFieldsSwappingManager<FogRendererContext> swappingManager;
    
    public static void init() {
        //load the class and apply mixin
        FogRenderer.class.hashCode();
        
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
                ResourceKey<Level> dimension = world.dimension();
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
    
    public static Vec3 getFogColorOf(
        ClientLevel destWorld, Vec3 pos
    ) {
        Minecraft client = Minecraft.getInstance();
        
        client.getProfiler().push("get_fog_color");
        
        ClientLevel oldWorld = client.level;
        
        ResourceKey<Level> newWorldKey = destWorld.dimension();
        
        swappingManager.contextMap.computeIfAbsent(
            newWorldKey,
            k -> new StaticFieldsSwappingManager.ContextRecord<>(
                k, new FogRendererContext(), true
            )
        );
        
        swappingManager.pushSwapping(newWorldKey);
        client.level = destWorld;
        
        Camera newCamera = new Camera();
        ((IECamera) newCamera).portal_setPos(pos);
        ((IECamera) newCamera).portal_setFocusedEntity(client.cameraEntity);
        
        try {
            FogRenderer.setupColor(
                newCamera,
                RenderStates.tickDelta,
                destWorld,
                client.options.getEffectiveRenderDistance(),
                client.gameRenderer.getDarkenWorldAmount(RenderStates.tickDelta)
            );
            
            Vec3 result = getCurrentFogColor.get();
            
            return result;
        }
        finally {
            swappingManager.popSwapping();
            client.level = oldWorld;
            
            client.getProfiler().pop();
        }
    }
    
    public static void onPlayerTeleport(ResourceKey<Level> from, ResourceKey<Level> to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }
    
}
