package com.qouteall.immersive_portals.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;

public class AltiusGeneratorType {
    public static LevelGeneratorTypeConstructor constructor;
    
    public static LevelGeneratorType generatorType;
    
    public static void init() {
        LevelGeneratorType[] types = LevelGeneratorType.TYPES;
        
        int generatorTypeId = allocateGeneratorTypeId(types);
        
        generatorType = constructor.construct(
            generatorTypeId,
            "imm_ptl_altius",
            "imm_ptl_altius",
            0
        );
        
    }
    
    private static int allocateGeneratorTypeId(LevelGeneratorType[] types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == null) {
                return i;
            }
        }
        throw new IllegalStateException(
            "Level Generator Type Id is Not Enough?"
        );
    }
    
    private static void createPortals() {
        VerticalConnectingPortal.connectMutually(
            DimensionType.OVERWORLD,
            DimensionType.THE_NETHER,
            0, 128
        );
        
        VerticalConnectingPortal.connectMutually(
            ModMain.alternate2,
            DimensionType.OVERWORLD,
            0, 256
        );
    }
    
    public static void initializePortalsIfNecessary() {
        GlobalPortalStorage gps =
            GlobalPortalStorage.get(McHelper.getServer().getWorld(DimensionType.OVERWORLD));
        if (gps.data == null || gps.data.isEmpty()) {
            createPortals();
            Helper.log("Initialized Portals For Altius");
        }
    }
    
}
