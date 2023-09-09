package qouteall.imm_ptl.peripheral.platform_specific;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import qouteall.imm_ptl.peripheral.PeripheralModMain;

public class PeripheralModEntry implements ModInitializer {
    
    
    @Override
    public void onInitialize() {
        PeripheralModMain.registerBlocks((id1, block1) -> Registry.register(
            BuiltInRegistries.BLOCK,
            id1,
            block1
        ));
        PeripheralModMain.registerItems((id1, item1) -> Registry.register(
            BuiltInRegistries.ITEM,
            id1,
            item1
        ));
        
        PeripheralModMain.init();
    }
}
