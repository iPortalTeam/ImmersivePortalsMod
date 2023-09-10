package qouteall.imm_ptl.peripheral;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.alternate_dimension.ChaosBiomeSource;
import qouteall.imm_ptl.peripheral.alternate_dimension.ErrorTerrainGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.FormulaGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.NormalSkylandGenerator;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;
import qouteall.imm_ptl.peripheral.wand.ClientPortalWandPortalDrag;
import qouteall.imm_ptl.peripheral.wand.PortalWandInteraction;
import qouteall.imm_ptl.peripheral.wand.PortalWandItem;
import qouteall.q_misc_util.LifecycleHack;

import java.util.function.BiConsumer;

public class PeripheralModMain {
    
    public static final Block portalHelperBlock =
        new Block(FabricBlockSettings.of().noOcclusion().isRedstoneConductor((a, b, c) -> false));
    
    public static final BlockItem portalHelperBlockItem =
        new PortalHelperItem(PeripheralModMain.portalHelperBlock, new Item.Properties());
    
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        IPOuterClientMisc.initClient();
        
        PortalWandItem.initClient();
        
        ClientPortalWandPortalDrag.init();
    }
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        DimStackManagement.init();
        
        AlternateDimensions.init();
        
        LifecycleHack.markNamespaceStable("immersive_portals");
        LifecycleHack.markNamespaceStable("imm_ptl");
        
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            new ResourceLocation("immersive_portals:error_terrain_generator"),
            ErrorTerrainGenerator.codec
        );
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            new ResourceLocation("immersive_portals:normal_skyland_generator"),
            NormalSkylandGenerator.codec
        );
        
        Registry.register(
            BuiltInRegistries.BIOME_SOURCE,
            new ResourceLocation("immersive_portals:chaos_biome_source"),
            ChaosBiomeSource.CODEC
        );
        
        PortalWandItem.init();
        
        CommandStickItem.init();
        
        PortalWandInteraction.init();
        
        CommandStickItem.registerCommandStickTypes();
        
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(PeripheralModMain.portalHelperBlockItem);
        });
    }
    
    public static void registerItems(BiConsumer<ResourceLocation, Item> regFunc) {
        regFunc.accept(
            new ResourceLocation("immersive_portals", "portal_helper"),
            portalHelperBlockItem
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals:command_stick"),
            CommandStickItem.instance
        );
        
        regFunc.accept(
            new ResourceLocation("immersive_portals:portal_wand"),
            PortalWandItem.instance
        );
    }
    
    public static void registerBlocks(BiConsumer<ResourceLocation, Block> regFunc) {
        regFunc.accept(
            new ResourceLocation("immersive_portals", "portal_helper"),
            portalHelperBlock
        );
    }
}
