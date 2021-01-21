package com.qouteall.hiding_in_the_bushes;

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
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MyRegistry {
    public static void registerMyDimensionsFabric() {
    }
    
    public static void registerBlocksFabric() {
        PortalPlaceholderBlock.instance = new PortalPlaceholderBlock(
            FabricBlockSettings.of(Material.PORTAL)
                .noCollision()
                .sounds(BlockSoundGroup.GLASS)
                .strength(1.0f, 0)
                .nonOpaque()
                .dropsNothing()
                .luminance(15)
        );
        Registry.register(
            Registry.BLOCK,
            // the id is inappropriate
            new Identifier("immersive_portals", "nether_portal_block"),
            PortalPlaceholderBlock.instance
        );
        
        
    }
    
    private static <T extends Entity> void registerEntity(
        Consumer<EntityType<T>> setEntityType,
        Supplier<EntityType<T>> getEntityType,
        String id,
        EntityType.EntityFactory<T> constructor,
        Registry<EntityType<?>> registry
    ) {
        EntityType<T> entityType = FabricEntityTypeBuilder.create(
            SpawnGroup.MISC,
            constructor
        ).dimensions(
            new EntityDimensions(1, 1, true)
        ).fireImmune().trackable(96, 20).build();
        setEntityType.accept(entityType);
        Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(id),
            entityType
        );
    }
    
    public static void registerEntitiesFabric() {
        DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;
        
        registerEntity(
            o -> Portal.entityType = o,
            () -> Portal.entityType,
            "immersive_portals:portal",
            Portal::new,
            registry
        );
        registerEntity(
            o -> NetherPortalEntity.entityType = o,
            () -> NetherPortalEntity.entityType,
            "immersive_portals:nether_portal_new",
            NetherPortalEntity::new,
            registry
        );
        
        registerEntity(
            o -> EndPortalEntity.entityType = o,
            () -> EndPortalEntity.entityType,
            "immersive_portals:end_portal",
            EndPortalEntity::new,
            registry
        );
        
        registerEntity(
            o -> Mirror.entityType = o,
            () -> Mirror.entityType,
            "immersive_portals:mirror",
            Mirror::new,
            registry
        );
        
        registerEntity(
            o -> BreakableMirror.entityType = o,
            () -> BreakableMirror.entityType,
            "immersive_portals:breakable_mirror",
            BreakableMirror::new,
            registry
        );
        
        registerEntity(
            o -> GlobalTrackedPortal.entityType = o,
            () -> GlobalTrackedPortal.entityType,
            "immersive_portals:global_tracked_portal",
            GlobalTrackedPortal::new,
            registry
        );
        
        registerEntity(
            o -> WorldWrappingPortal.entityType = o,
            () -> WorldWrappingPortal.entityType,
            "immersive_portals:border_portal",
            WorldWrappingPortal::new,
            registry
        );
        
        registerEntity(
            o -> VerticalConnectingPortal.entityType = o,
            () -> VerticalConnectingPortal.entityType,
            "immersive_portals:end_floor_portal",
            VerticalConnectingPortal::new,
            registry
        );
        
        registerEntity(
            o -> GeneralBreakablePortal.entityType = o,
            () -> GeneralBreakablePortal.entityType,
            "immersive_portals:general_breakable_portal",
            GeneralBreakablePortal::new,
            registry
        );
        
        LoadingIndicatorEntity.entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "loading_indicator"),
            FabricEntityTypeBuilder.create(
                SpawnGroup.MISC,
                (EntityType.EntityFactory<LoadingIndicatorEntity>) LoadingIndicatorEntity::new
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune().trackable(96, 20).build()
        );
    }
    
    public static void registerChunkGenerators() {
        //it should not be serialized
//        Registry.register(
//            Registry.CHUNK_GENERATOR,
//            new Identifier("immersive_portals:error_terrain_gen"),
//            ErrorTerrainGenerator.codec
//        );
        
    }
}
