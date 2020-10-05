package com.qouteall.hiding_in_the_bushes;

import com.qouteall.imm_ptl_peripheral.PeripheralModMain;
import net.fabricmc.api.ModInitializer;

public class PeripheralModEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        
        PeripheralModMain.init();
    }
}
