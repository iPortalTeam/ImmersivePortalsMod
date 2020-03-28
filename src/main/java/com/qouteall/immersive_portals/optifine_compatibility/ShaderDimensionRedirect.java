package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;

public class ShaderDimensionRedirect {
    private static Map<String, String> redirectMap = new HashMap<>();
    
    public static void init() {
        redirectMap.put("immersive_portals:alternate1", "minecraft:overworld");
        redirectMap.put("immersive_portals:alternate2", "minecraft:overworld");
        redirectMap.put("immersive_portals:alternate3", "minecraft:overworld");
        redirectMap.put("immersive_portals:alternate4", "minecraft:overworld");
        redirectMap.put("immersive_portals:alternate5", "minecraft:overworld");
    }
    
    public static DimensionType getShaderDimension(DimensionType dimension) {
        String stringId = DimensionType.getId(dimension).toString();
        if (redirectMap.containsKey(stringId)) {
            String redirected = redirectMap.get(stringId);
            DimensionType redirectedDim = DimensionType.byId(new Identifier(redirected));
            if (redirectedDim == null) {
                Helper.err("Redirected Dimension Invalid " + redirected);
                return DimensionType.OVERWORLD;
            }
            else {
                return redirectedDim;
            }
        }
        else {
            return dimension;
        }
    }
}
