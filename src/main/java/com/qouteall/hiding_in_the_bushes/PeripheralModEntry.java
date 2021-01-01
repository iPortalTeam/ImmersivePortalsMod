package com.qouteall.hiding_in_the_bushes;

import com.qouteall.imm_ptl_peripheral.PeripheralModMain;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class PeripheralModEntry implements ModInitializer {
    static void registerPortalHelperBlock() {
        PeripheralModMain.portalHelperBlock = new Block(FabricBlockSettings.of(Material.METAL).nonOpaque()
            .solidBlock((a, b, c) -> false));
        
        PeripheralModMain.portalHelperBlockItem = new BlockItem(
            PeripheralModMain.portalHelperBlock,
            new Item.Settings().group(ItemGroup.MISC)
        );
        
        Registry.register(
            Registry.BLOCK,
            new Identifier("immersive_portals", "portal_helper"),
            PeripheralModMain.portalHelperBlock
        );
        
        Registry.register(
            Registry.ITEM,
            new Identifier("immersive_portals", "portal_helper"),
            PeripheralModMain.portalHelperBlockItem
        );
    }
    
    @Override
    public void onInitialize() {
        PeripheralModEntry.registerPortalHelperBlock();
        
        PeripheralModMain.init();
    }
}
