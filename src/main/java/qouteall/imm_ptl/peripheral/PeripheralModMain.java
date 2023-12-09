package qouteall.imm_ptl.peripheral;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
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
    
    public static final CreativeModeTab TAB =
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(PortalWandItem.instance))
            .title(Component.translatable("imm_ptl.item_group"))
            .displayItems((enabledFeatures, entries) -> {
                PortalWandItem.addIntoCreativeTag(entries);
                
                CommandStickItem.addIntoCreativeTag(entries);
                
                entries.accept(PeripheralModMain.portalHelperBlockItem);
            })
            .build();
    
    @Environment(EnvType.CLIENT)
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
        
        PortalWandItem.init();
        
        CommandStickItem.init();
        
        PortalWandInteraction.init();
        
        CommandStickItem.registerCommandStickTypes();
        
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
    
    public static void registerChunkGenerators(
        BiConsumer<ResourceLocation, Codec<? extends ChunkGenerator>> regFunc
    ) {
        regFunc.accept(
            new ResourceLocation("immersive_portals:error_terrain_generator"),
            ErrorTerrainGenerator.codec
        );
        regFunc.accept(
            new ResourceLocation("immersive_portals:normal_skyland_generator"),
            NormalSkylandGenerator.codec
        );
    }
    
    public static void registerBiomeSources(
        BiConsumer<ResourceLocation, Codec<? extends BiomeSource>> regFunc
    ) {
        regFunc.accept(
            new ResourceLocation("immersive_portals:chaos_biome_source"),
            ChaosBiomeSource.CODEC
        );
    }
    
    public static void registerCreativeTabs(
        BiConsumer<ResourceLocation, CreativeModeTab> regFunc
    ) {
        regFunc.accept(
            new ResourceLocation("immersive_portals", "general"),
            TAB
        );
    }
}
