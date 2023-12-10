package qouteall.imm_ptl.peripheral.alternate_dimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.jetbrains.annotations.Nullable;
import qouteall.dimlib.DimensionTemplate;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackInfo;
import qouteall.imm_ptl.peripheral.dim_stack.DimensionStackAPI;

import java.util.List;
import java.util.Optional;

public class AlternateDimensions {
    
    public static final ResourceKey<DimensionType> SURFACE_TYPE = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        new ResourceLocation("immersive_portals:surface_type")
    );
    
    public static final ResourceKey<DimensionType> SURFACE_TYPE_BRIGHT = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        new ResourceLocation("immersive_portals:surface_type_bright")
    );
    
    public static final ResourceKey<Level> SKYLAND = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:skyland")
    );
    
    public static final ResourceKey<Level> BRIGHT_SKYLAND = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:bright_skyland")
    );
    
    public static final ResourceKey<Level> CHAOS = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:chaos")
    );
    
    public static final ResourceKey<Level> VOID = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:void")
    );
    
    public static final ResourceKey<Level> BRIGHT_VOID = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("immersive_portals:bright_void")
    );
    
    public static final DimensionTemplate SKYLAND_TEMPLATE = new DimensionTemplate(
        SURFACE_TYPE,
        (server, dimensionTypeHolder) -> new LevelStem(
            dimensionTypeHolder,
            createSkylandGenerator(
                server.registryAccess(), server.getWorldData().worldGenOptions().seed()
            )
        )
    );
    
    public static final DimensionTemplate BRIGHT_SKYLAND_TEMPLATE = new DimensionTemplate(
        SURFACE_TYPE_BRIGHT,
        (server, dimensionTypeHolder) -> new LevelStem(
            dimensionTypeHolder,
            createSkylandGenerator(
                server.registryAccess(), server.getWorldData().worldGenOptions().seed()
            )
        )
    );
    
    public static final DimensionTemplate CHAOS_TEMPLATE = new DimensionTemplate(
        SURFACE_TYPE,
        (server, dimensionTypeHolder) -> new LevelStem(
            dimensionTypeHolder,
            createErrorTerrainGenerator(
                server.getWorldData().worldGenOptions().seed(),
                server.registryAccess()
            )
        )
    );
    
    public static final DimensionTemplate BRIGHT_VOID_TEMPLATE = new DimensionTemplate(
        SURFACE_TYPE_BRIGHT,
        (server, dimensionTypeHolder) -> new LevelStem(
            dimensionTypeHolder,
            createVoidGenerator(server.registryAccess())
        )
    );
    
    
    public static void init() {
        DimensionStackAPI.DIMENSION_STACK_CANDIDATE_COLLECTION_EVENT.register(
            (registryAccess, options) -> {
                return List.of(BRIGHT_SKYLAND, SKYLAND, CHAOS, VOID);
            }
        );
        
        DimensionStackAPI.DIMENSION_STACK_PRE_UPDATE_EVENT.register(
            AlternateDimensions::addAltDimsIfUsedInDimStack
        );
        
        ServerTickEvents.END_SERVER_TICK.register(AlternateDimensions::tick);
        
        DimensionTemplate.registerDimensionTemplate(
            "skyland", SKYLAND_TEMPLATE
        );
        DimensionTemplate.registerDimensionTemplate(
            "bright_skyland", BRIGHT_SKYLAND_TEMPLATE
        );
        DimensionTemplate.registerDimensionTemplate(
            "chaos", CHAOS_TEMPLATE
        );
        DimensionTemplate.registerDimensionTemplate(
            "bright_void", BRIGHT_VOID_TEMPLATE
        );
    }
    
    private static void addAltDimsIfUsedInDimStack(
        MinecraftServer server, @Nullable DimStackInfo dimStackInfo
    ) {
        if (dimStackInfo == null) {
            return;
        }
        
        if (dimStackInfo.hasDimension(BRIGHT_SKYLAND)) {
            DimensionAPI.addDimensionIfNotExists(
                server,
                BRIGHT_SKYLAND.location(),
                () -> BRIGHT_SKYLAND_TEMPLATE.createLevelStem(server)
            );
        }
        
        if (dimStackInfo.hasDimension(SKYLAND)) {
            DimensionAPI.addDimensionIfNotExists(
                server,
                SKYLAND.location(),
                () -> SKYLAND_TEMPLATE.createLevelStem(server)
            );
        }
        
        if (dimStackInfo.hasDimension(CHAOS)) {
            DimensionAPI.addDimensionIfNotExists(
                server,
                CHAOS.location(),
                () -> CHAOS_TEMPLATE.createLevelStem(server)
            );
        }
        
        if (dimStackInfo.hasDimension(VOID)) {
            DimensionAPI.addDimensionIfNotExists(
                server,
                VOID.location(),
                () -> DimensionTemplate.VOID_TEMPLATE.createLevelStem(server)
            );
        }
        
        if (dimStackInfo.hasDimension(BRIGHT_VOID)) {
            DimensionAPI.addDimensionIfNotExists(
                server,
                BRIGHT_VOID.location(),
                () -> BRIGHT_VOID_TEMPLATE.createLevelStem(server)
            );
        }
        
    }
    
    public static boolean isAlternateDimension(Level world) {
        ResourceKey<DimensionType> dimensionTypeId = world.dimensionTypeId();
        return dimensionTypeId == SURFACE_TYPE
            || dimensionTypeId == SURFACE_TYPE_BRIGHT;
    }
    
    public static ChunkGenerator createSkylandGenerator(RegistryAccess rm, long seed) {
        return NormalSkylandGenerator.create(
            rm.registryOrThrow(Registries.BIOME).asLookup(),
            rm.registryOrThrow(Registries.DENSITY_FUNCTION).asLookup(),
            rm.registryOrThrow(Registries.NOISE).asLookup(),
            rm.registryOrThrow(Registries.NOISE_SETTINGS).asLookup(),
            rm.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).asLookup(),
            seed
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, RegistryAccess rm) {
        return ErrorTerrainGenerator.create(
            rm.registryOrThrow(Registries.BIOME).asLookup(),
            rm.registryOrThrow(Registries.NOISE_SETTINGS).asLookup()
        );
    }
    
    public static ChunkGenerator createVoidGenerator(RegistryAccess rm) {
        Registry<Biome> biomeRegistry = rm.registryOrThrow(Registries.BIOME);
        
        Holder.Reference<Biome> plainsHolder = biomeRegistry.getHolderOrThrow(Biomes.PLAINS);
        
        FlatLevelGeneratorSettings flatChunkGeneratorConfig =
            new FlatLevelGeneratorSettings(
                Optional.of(HolderSet.direct()),
                plainsHolder,
                List.of()
            );
        flatChunkGeneratorConfig.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
        flatChunkGeneratorConfig.updateLayers();
        
        return new FlatLevelSource(flatChunkGeneratorConfig);
    }
    
    private static void tick(MinecraftServer server) {
        for (ServerLevel world : server.getAllLevels()) {
            if (isAlternateDimension(world)) {
                syncWeatherFromOverworld(
                    world, McHelper.getOverWorldOnServer()
                );
            }
        }
    }
    
    private static void syncWeatherFromOverworld(
        ServerLevel world, ServerLevel overworld
    ) {
        ((IEWorld) world).portal_setWeather(
            overworld.getRainLevel(1), overworld.getRainLevel(1),
            overworld.getThunderLevel(1), overworld.getThunderLevel(1)
        );
    }
}
