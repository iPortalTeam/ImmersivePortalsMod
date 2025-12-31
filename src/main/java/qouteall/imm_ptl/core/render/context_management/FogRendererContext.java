package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ProfilerCompat;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import org.joml.Vector4f;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link FogRenderer}
 * {@link qouteall.imm_ptl.core.mixin.client.multiworld_awareness.MixinFogRenderer}
 */
@SuppressWarnings("SpellCheckingInspection")
public class FogRendererContext {
    public float red;
    public float green;
    public float blue;
    public int targetBiomeFog = -1;
    public int previousBiomeFog = -1;
    public long biomeChangedTime = -1L;
    public boolean fogEnabled = true;
    
    public static Consumer<FogRendererContext> copyContextFromObject;
    public static Consumer<FogRendererContext> copyContextToObject;
    public static Supplier<Vec3> getCurrentFogColor;

    private static Vec3 currentFogColor = Vec3.ZERO;
    
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
        
        ProfilerCompat.push("get_fog_color");
        
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
        ((IECamera) newCamera).portal_setFocusedEntity(client.getCameraEntity());
        
        try {
            IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
            FogRenderer fogRenderer = ieGameRenderer.ip_getFogRenderer();
            Vector4f fogColor = fogRenderer.setupFog(
                newCamera,
                client.options.getEffectiveRenderDistance(),
                client.getDeltaTracker(),
                client.gameRenderer.getDarkenWorldAmount(RenderStates.getPartialTick()),
                destWorld
            );
            Vec3 result = new Vec3(fogColor.x(), fogColor.y(), fogColor.z());
            setCurrentFogColor(fogColor);
            return result;
        }
        finally {
            swappingManager.popSwapping();
            client.level = oldWorld;
            
            ProfilerCompat.pop();
        }
    }
    
    public static void onPlayerTeleport(ResourceKey<Level> from, ResourceKey<Level> to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }

    public static void setCurrentFogColor(Vector4f fogColor) {
        currentFogColor = new Vec3(fogColor.x(), fogColor.y(), fogColor.z());
    }

    public static Vec3 getCurrentFogColorValue() {
        return currentFogColor;
    }
    
}
