package qouteall.imm_ptl.peripheral.platform_specific;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import qouteall.imm_ptl.peripheral.PeripheralModMain;

public class PeripheralModEntryClient implements ClientModInitializer {
    public static void registerBlockRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(
            PeripheralModMain.portalHelperBlock,
            RenderType.cutout()
        );
    }
    
    @Override
    public void onInitializeClient() {
        PeripheralModEntryClient.registerBlockRenderLayers();
        
        PeripheralModMain.initClient();
    }
}
