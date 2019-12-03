package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.IEOFWorldRenderer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

import java.lang.reflect.Field;
import java.util.ArrayList;

@Mixin(value = WorldRenderer.class, remap = false)
public class MOWorldRenderer implements IEOFWorldRenderer {
    //it seems that it can't shadow a field that optifine adds
    //so use reflection
    private static Field f_renderInfosNormal;
    
    @Override
    public void createNewRenderInfosNormal() {
        try {
            f_renderInfosNormal.set(this, new ArrayList<>(512));
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
    
    static {
        try {
            f_renderInfosNormal = WorldRenderer.class.getDeclaredField("renderInfosNormal");
        }
        catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }


//    @Shadow
//    private List renderInfosNormal;
//
//    @Override
//    public void createNewRenderInfosNormal() {
//        renderInfosNormal = new ArrayList(512);
//    }
}
