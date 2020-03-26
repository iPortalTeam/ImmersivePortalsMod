package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public class CrossPortalEntityRenderer {
    //there is no weak hash set
    private static final WeakHashMap<Entity, Object> collidedEntities = new WeakHashMap<>();
    
    public static void init() {
        ModMain.postClientTickSignal.connect(CrossPortalEntityRenderer::onClientTick);
    }
    
    private static void onClientTick() {
        collidedEntities.entrySet().removeIf(entry ->
            ((IEEntity) entry.getKey()).getCollidingPortal() == null
        );
    }
    
    public static void onEntityTickClient(Entity entity){
        if (((IEEntity) entity).getCollidingPortal() != null) {
            collidedEntities.put(entity, null);
        }
    }
    
    public static void beforeRenderingEntity(Entity entity) {
    
    }
    
    public static void afterRenderingEntity(Entity entity) {
    
    }
}
