package com.qouteall.hiding_in_the_bushes;

import com.qouteall.imm_ptl_peripheral.guide.IPGuide;
import net.fabricmc.api.ClientModInitializer;

public class PeripheralModEntryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IPGuide.initClient();
    }
}
