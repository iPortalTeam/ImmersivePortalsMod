package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.optifine_compatibility.IEOFWorldRenderer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldRenderer.class)
public class MOWorldRenderer implements IEOFWorldRenderer {
    @Shadow
    private List renderInfosNormal;
    
    @Override
    public void createNewRenderInfosNormal() {
        renderInfosNormal = new ArrayList(512);
    }
}
