package qouteall.imm_ptl.peripheral.platform_specific;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import qouteall.imm_ptl.peripheral.PeripheralModMain;

public class PeripheralModEntry implements ModInitializer {
    
    
    @Override
    public void onInitialize() {
        PeripheralModMain.registerBlocks((id, ele) -> Registry.register(
            BuiltInRegistries.BLOCK, id, ele
        ));
        PeripheralModMain.registerItems((id, ele) -> Registry.register(
            BuiltInRegistries.ITEM, id, ele
        ));
        PeripheralModMain.registerChunkGenerators((id, ele) -> Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR, id, ele
        ));
        PeripheralModMain.registerBiomeSources((id, ele) -> Registry.register(
            BuiltInRegistries.BIOME_SOURCE, id, ele
        ));
        PeripheralModMain.registerCreativeTabs((id, ele) -> Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, id, ele
        ));
        
        PeripheralModMain.init();
    }
}
