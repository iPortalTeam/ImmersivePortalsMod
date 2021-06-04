package qouteall.imm_ptl.peripheral.platform_specific;

import qouteall.imm_ptl.peripheral.PeripheralModMain;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public class PeripheralModEntryClient implements ClientModInitializer {
    public static void registerBlockRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(
            PeripheralModMain.portalHelperBlock,
            RenderLayer.getCutout()
        );
    }
    
    @Override
    public void onInitializeClient() {
        PeripheralModEntryClient.registerBlockRenderLayers();
        
        PeripheralModMain.initClient();
    }
}
