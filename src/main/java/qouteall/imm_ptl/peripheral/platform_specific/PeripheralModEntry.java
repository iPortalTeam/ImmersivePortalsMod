package qouteall.imm_ptl.peripheral.platform_specific;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import qouteall.imm_ptl.peripheral.CommandStickItem;
import qouteall.imm_ptl.peripheral.PeripheralModMain;
import qouteall.imm_ptl.peripheral.guide.IPGuide;

import javax.annotation.Nullable;
import java.util.List;

public class PeripheralModEntry implements ModInitializer {
    public static class PortalHelperItem extends BlockItem {
        
        public PortalHelperItem(Block block, Properties settings) {
            super(block, settings);
        }
        
        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (context.getLevel().isClientSide()) {
                if (context.getPlayer() != null) {
                    IPGuide.onClientPlacePortalHelper();
                }
            }
            
            return super.useOn(context);
        }
        
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
            super.appendHoverText(stack, world, tooltip, context);
            
            tooltip.add(Component.translatable("imm_ptl.portal_helper_tooltip"));
        }
    }
    
    private static void registerBlockItems() {
        PeripheralModMain.portalHelperBlock = new Block(FabricBlockSettings.of(Material.METAL).noOcclusion()
            .isRedstoneConductor((a, b, c) -> false));
        
        PeripheralModMain.portalHelperBlockItem = new PortalHelperItem(
            PeripheralModMain.portalHelperBlock,
            new Item.Properties().tab(CreativeModeTab.TAB_MISC)
        );
        
        Registry.register(
            Registry.BLOCK,
            new ResourceLocation("immersive_portals", "portal_helper"),
            PeripheralModMain.portalHelperBlock
        );
        
        Registry.register(
            Registry.ITEM,
            new ResourceLocation("immersive_portals", "portal_helper"),
            PeripheralModMain.portalHelperBlockItem
        );
        
        Registry.register(
            Registry.ITEM,
            new ResourceLocation("immersive_portals:command_stick"),
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
