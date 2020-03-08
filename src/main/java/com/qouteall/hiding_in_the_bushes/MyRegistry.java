package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.render.LoadingIndicatorRenderer;
import com.qouteall.immersive_portals.render.PortalEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensionType;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public class MyRegistry {
    public static void registerMyDimensionsFabric() {
        ModMain.alternate1 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGenerator1,
                () -> ModMain.alternate1
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate1"));
        
        ModMain.alternate2 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGenerator2,
                () -> ModMain.alternate2
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate2"));
        
        ModMain.alternate3 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGenerator3,
                () -> ModMain.alternate3
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate3"));
        
        ModMain.alternate4 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGenerator4,
                () -> ModMain.alternate4
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate4"));
        
        ModMain.alternate5 = FabricDimensionType.builder()
            .factory((world, type) -> new AlternateDimension(
                world, type, AlternateDimension::getChunkGenerator5,
                () -> ModMain.alternate5
            ))
            .skyLight(true)
            .defaultPlacer(
                (teleported, destination, portalDir, horizontalOffset, verticalOffset) ->
                    new BlockPattern.TeleportTarget(
                        Vec3d.ZERO,
                        Vec3d.ZERO,
                        0
                    )
            )
            .buildAndRegister(new Identifier("immersive_portals", "alternate5"));
    }
    
    public static void registerBlocksFabric() {
        PortalPlaceholderBlock.instance = new PortalPlaceholderBlock(
            FabricBlockSettings.of(Material.PORTAL)
                .noCollision()
                .sounds(BlockSoundGroup.GLASS)
                .strength(99999, 0)
                .lightLevel(15)
                .ticksRandomly()
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
                EntityCategory.MISC,
                (EntityType<Portal> type, World world1) ->
                    new Portal(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        NewNetherPortalEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "nether_portal_new"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<NewNetherPortalEntity> type, World world1) ->
                    new NewNetherPortalEntity(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        EndPortalEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "end_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType.EntityFactory<EndPortalEntity>) EndPortalEntity::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        Mirror.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "mirror"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<Mirror> type, World world1) ->
                    new Mirror(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        BreakableMirror.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "breakable_mirror"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<BreakableMirror> type, World world1) ->
                    new BreakableMirror(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        GlobalTrackedPortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "global_tracked_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                GlobalTrackedPortal::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        BorderPortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "border_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                BorderPortal::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        VerticalConnectingPortal.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "end_floor_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                VerticalConnectingPortal::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
        
        LoadingIndicatorEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "loading_indicator"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType.EntityFactory<LoadingIndicatorEntity>) LoadingIndicatorEntity::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).build()
        );
    }
    
    @Environment(EnvType.CLIENT)
    public static void initPortalRenderers() {
    
        Arrays.stream(new EntityType<?>[]{
            Portal.entityType,
            NewNetherPortalEntity.entityType,
            EndPortalEntity.entityType,
            Mirror.entityType,
            BreakableMirror.entityType,
            GlobalTrackedPortal.entityType,
            BorderPortal.entityType,
            VerticalConnectingPortal.entityType
        }).peek(
            Validate::notNull
        ).forEach(
            entityType -> EntityRendererRegistry.INSTANCE.register(
                entityType,
                (entityRenderDispatcher, context) -> new PortalEntityRenderer(entityRenderDispatcher)
            )
        );
    
        EntityRendererRegistry.INSTANCE.register(
            LoadingIndicatorEntity.entityType,
            (entityRenderDispatcher, context) -> new LoadingIndicatorRenderer(entityRenderDispatcher)
        );
    
    }
    
    public static void registerEffectAndPotion() {
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
                HandReachTweak.longerReachEffect, 3600, 1
            )
        );
        Registry.register(
            Registry.POTION,
            new Identifier("immersive_portals", "longer_reach_potion"),
            HandReachTweak.longerReachPotion
        );
    }
}
