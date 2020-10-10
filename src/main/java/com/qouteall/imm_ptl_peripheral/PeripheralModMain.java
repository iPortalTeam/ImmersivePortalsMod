package com.qouteall.imm_ptl_peripheral;

import com.qouteall.imm_ptl_peripheral.alternate_dimension.FormulaGenerator;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusManagement;
import com.qouteall.imm_ptl_peripheral.portal_generation.IntrinsicPortalGeneration;

public class PeripheralModMain {
    
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        AltiusGameRule.init();
        AltiusManagement.init();
    }
    
}
