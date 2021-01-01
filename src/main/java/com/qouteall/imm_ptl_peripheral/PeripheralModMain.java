package com.qouteall.imm_ptl_peripheral;

import com.qouteall.imm_ptl_peripheral.alternate_dimension.AlternateDimensions;
import com.qouteall.imm_ptl_peripheral.alternate_dimension.FormulaGenerator;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusManagement;
import com.qouteall.imm_ptl_peripheral.guide.IPGuide;
import com.qouteall.imm_ptl_peripheral.portal_generation.IntrinsicPortalGeneration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;

public class PeripheralModMain {
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGuide.initClient();
    }
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        AltiusGameRule.init();
        AltiusManagement.init();
        
        AlternateDimensions.init();
    }
    
}
