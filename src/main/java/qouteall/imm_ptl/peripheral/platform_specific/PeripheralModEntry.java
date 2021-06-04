package qouteall.imm_ptl.peripheral.platform_specific;

import qouteall.imm_ptl.peripheral.CommandStickItem;
import qouteall.imm_ptl.peripheral.PeripheralModMain;
import qouteall.imm_ptl.peripheral.guide.IPGuide;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class PeripheralModEntry implements ModInitializer {
    public static class PortalHelperItem extends BlockItem {
        
        public PortalHelperItem(Block block, Settings settings) {
            super(block, settings);
        }
        
        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            if (context.getWorld().isClient()) {
                if (context.getPlayer() != null) {
                    IPGuide.onClientPlacePortalHelper();
                }
            }
            
            return super.useOnBlock(context);
        }
        
        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, world, tooltip, context);
            
            tooltip.add(new TranslatableText("imm_ptl.portal_helper_tooltip"));
        }
    }
    
    private static void registerBlockItems() {
        PeripheralModMain.portalHelperBlock = new Block(FabricBlockSettings.of(Material.METAL).nonOpaque()
            .solidBlock((a, b, c) -> false));
        
        PeripheralModMain.portalHelperBlockItem = new PortalHelperItem(
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
        
        Registry.register(
            Registry.ITEM,
            new Identifier("immersive_portals:command_stick"),
            CommandStickItem.instance
        );
    
        PeripheralModMain.registerCommandStickTypes();
        
        CommandStickItem.init();
    }
    
    @Override
    public void onInitialize() {
        PeripheralModEntry.registerBlockItems();
        
        PeripheralModMain.init();
    }
}
