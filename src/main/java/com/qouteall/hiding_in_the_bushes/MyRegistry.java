package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.global_portals.WorldWrappingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.potion.Potion;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MyRegistry {
    public static void registerMyDimensionsFabric() {
    }
    
    public static void registerBlocksFabric() {
        PortalPlaceholderBlock.instance = new PortalPlaceholderBlock(
            FabricBlockSettings.of(Material.PORTAL)
                .noCollision()
                .sounds(BlockSoundGroup.GLASS)
                .strength(99999, 0)
                .lightLevel(15)
                .nonOpaque()
                .dropsNothing()
                .build()
        );
        Registry.register(
            Registry.BLOCK,
            //the id is not appropriate because I did not implement end portal when making this block
            new Identifier("immersive_portals", "nether_portal_block"),
            PortalPlaceholderBlock.instance
        );
        
        ModMain.portalHelperBlock = new Block(FabricBlockSettings.of(Material.METAL).build());
        
        ModMain.portalHelperBlockItem = new BlockItem(
            ModMain.portalHelperBlock,
            new Item.Settings().group(ItemGroup.MISC)
        );
        
        Registry.register(
            Registry.BLOCK,
            new Identifier("immersive_portals", "portal_helper"),
            ModMain.portalHelperBlock
        );
        
        Registry.register(
            Registry.ITEM,
            new Identifier("immersive_portals", "portal_helper"),
            ModMain.portalHelperBlockItem
        );
    }
    
    public static void registerEntitiesFabric() {
        Portal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "portal"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                Portal::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        NetherPortalEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "nether_portal_new"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                NetherPortalEntity::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        EndPortalEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "end_portal"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                (EntityType.EntityFactory<EndPortalEntity>) EndPortalEntity::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        Mirror.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "mirror"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                Mirror::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        BreakableMirror.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "breakable_mirror"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                BreakableMirror::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        GlobalTrackedPortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "global_tracked_portal"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                GlobalTrackedPortal::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        WorldWrappingPortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "border_portal"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                WorldWrappingPortal::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        VerticalConnectingPortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "end_floor_portal"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                VerticalConnectingPortal::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    
        GeneralBreakablePortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "general_breakable_portal"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                GeneralBreakablePortal::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
        
        LoadingIndicatorEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "loading_indicator"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                (EntityType.EntityFactory<LoadingIndicatorEntity>) LoadingIndicatorEntity::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96,20).build()
        );
    }
    
    public static void registerEffectAndPotion() {
        Registry.register(
            Registry.ATTRIBUTE,
            "immersive_portals:hand_reach_multiplier",
            HandReachTweak.handReachMultiplierAttribute
        );
        
        StatusEffect.class.hashCode();
        HandReachTweak.longerReachEffect = HandReachTweak.statusEffectConstructor.apply(
            StatusEffectType.BENEFICIAL, 0)
            .addAttributeModifier(
                HandReachTweak.handReachMultiplierAttribute,
                "91AEAA56-2333-2333-2333-2F7F68070635",
                0.5,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL
            );
        Registry.register(
            Registry.STATUS_EFFECT,
            new Identifier("immersive_portals", "longer_reach"),
            HandReachTweak.longerReachEffect
        );
        
        HandReachTweak.longerReachPotion = new Potion(
            new StatusEffectInstance(
                HandReachTweak.longerReachEffect, 7200, 1
            )
        );
        Registry.register(
            Registry.POTION,
            new Identifier("immersive_portals", "longer_reach_potion"),
            HandReachTweak.longerReachPotion
        );
    }
    
    public static void registerChunkGenerators() {
//        Registry.register(
//            Registry.CHUNK_GENERATOR,
//            new Identifier("immersive_portals:normal_skyland"),
//            NormalSkylandGenerator.codec
//        );
//
//        Registry.register(
//            Registry.CHUNK_GENERATOR,
//            new Identifier("immersive_portals:chaos_terrain"),
//            ErrorTerrainGenerator.codec
//        );
//
//        Registry.register(
//            Registry.CHUNK_GENERATOR,
//            new Identifier("immersive_portals:void_generator"),
//            VoidChunkGenerator.codec
//        );
    }
}
