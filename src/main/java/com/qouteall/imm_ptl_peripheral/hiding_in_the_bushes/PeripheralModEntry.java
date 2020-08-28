package com.qouteall.imm_ptl_peripheral.hiding_in_the_bushes;

import com.qouteall.imm_ptl_peripheral.alternate_dimension.FormulaGenerator;
import net.fabricmc.api.ModInitializer;
import net.logandark.languagehack.LanguageHack;

public class PeripheralModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        LanguageHack.activate("assets/immersive_portals");
    
        FormulaGenerator.init();
    }
}
