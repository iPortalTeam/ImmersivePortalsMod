package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelStorageSource;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunkAccess_AlternateDim;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunkGenerator_AlternateDim;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseRouterData;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * It extends NoiseBasedChunkGenerator, because in
 * {@link ChunkMap#ChunkMap(ServerLevel, LevelStorageSource.LevelStorageAccess, DataFixer, StructureTemplateManager, Executor, BlockableEventLoop, LightChunkGetter, ChunkGenerator, ChunkProgressListener, ChunkStatusUpdateListener, Supplier, int, boolean)}
 * it uses instanceof to initialize random source.
 */
public class NormalSkylandGenerator extends NoiseBasedChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(
        instance -> instance.group(
                RegistryOps.retrieveGetter(Registries.BIOME),
                RegistryOps.retrieveGetter(Registries.DENSITY_FUNCTION),
                RegistryOps.retrieveGetter(Registries.NOISE),
                RegistryOps.retrieveGetter(Registries.NOISE_SETTINGS),
                RegistryOps.retrieveGetter(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST),
                Codec.LONG.optionalFieldOf("seed", 0L).forGetter(g -> g.seed)
            )
            .apply(instance, NormalSkylandGenerator::create)
    );
    
    private RandomState delegatedRandomState;
    
    private final HolderGetter<Biome> biomeHolderGetter;
    private final HolderGetter<DensityFunction> densityFunctionHolderGetter;
    private final HolderGetter<NormalNoise.NoiseParameters> noiseParametersHolderGetter;
    private final long seed;
    
    public NormalSkylandGenerator(
        BiomeSource biomeSource,
        Holder<NoiseGeneratorSettings> noiseGeneratorSettings,
        
        NoiseBasedChunkGenerator delegate,
        
        HolderGetter<Biome> biomeHolderGetter,
        HolderGetter<DensityFunction> densityFunctionHolderGetter,
        HolderGetter<NormalNoise.NoiseParameters> noiseParametersHolderGetter,
        long seed
    ) {
        super(biomeSource, noiseGeneratorSettings);
        
        this.delegate = delegate;
        this.biomeHolderGetter = biomeHolderGetter;
        this.densityFunctionHolderGetter = densityFunctionHolderGetter;
        this.noiseParametersHolderGetter = noiseParametersHolderGetter;
        this.seed = seed;
        
        this.delegatedRandomState = RandomState.create(
            (delegate.generatorSettings().value()),
            ((HolderLookup.RegistryLookup<NormalNoise.NoiseParameters>) noiseParametersHolderGetter),
            seed
        );
    }
    
    public static NormalSkylandGenerator create(
        HolderGetter<Biome> biomeHolderGetter,
        HolderGetter<DensityFunction> densityFunctionHolderGetter,
        HolderGetter<NormalNoise.NoiseParameters> noiseParametersHolderGetter,
        HolderGetter<NoiseGeneratorSettings> noiseGeneratorSettingsHolderGetter,
        HolderGetter<MultiNoiseBiomeSourceParameterList> biomeParamListLookup,
        long seed
    ) {
        Holder.Reference<MultiNoiseBiomeSourceParameterList> overworldBiomeParamList =
            biomeParamListLookup.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        
        MultiNoiseBiomeSource overworldBiomeSource =
            MultiNoiseBiomeSource.createFromPreset(overworldBiomeParamList);
        
        NoiseGeneratorSettings overworldNGS = noiseGeneratorSettingsHolderGetter
            .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        
        NoiseGeneratorSettings intrinsicSkylandNGS = noiseGeneratorSettingsHolderGetter
            .getOrThrow(NoiseGeneratorSettings.FLOATING_ISLANDS).value();

//        NoiseGeneratorSettings endNGS = IENoiseGeneratorSettings.ip_end();
        
        NoiseGeneratorSettings usedSkylandNGS = new NoiseGeneratorSettings(
            intrinsicSkylandNGS.noiseSettings(),
            intrinsicSkylandNGS.defaultBlock(),
            intrinsicSkylandNGS.defaultFluid(),
            IENoiseRouterData.ip_noNewCaves(
                densityFunctionHolderGetter,
                noiseParametersHolderGetter,
                IENoiseRouterData.ip_slideEndLike(IENoiseRouterData.ip_getFunction(
                    densityFunctionHolderGetter, IENoiseRouterData.get_BASE_3D_NOISE_END()
                ), 0, 128)
            ),
            intrinsicSkylandNGS.surfaceRule(),
            intrinsicSkylandNGS.spawnTarget(),
            0, // overwrite seaLevel
            intrinsicSkylandNGS.disableMobGeneration(),
            intrinsicSkylandNGS.aquifersEnabled(),
            intrinsicSkylandNGS.oreVeinsEnabled(),
            intrinsicSkylandNGS.useLegacyRandomSource()
        );
        
        NoiseBasedChunkGenerator skylandGenerator = new NoiseBasedChunkGenerator(
            overworldBiomeSource, Holder.direct(usedSkylandNGS)
        );
        
        NormalSkylandGenerator result = new NormalSkylandGenerator(
            overworldBiomeSource,
            Holder.direct(overworldNGS),
            skylandGenerator,
            biomeHolderGetter,
            densityFunctionHolderGetter,
            noiseParametersHolderGetter,
            seed
        );
        
        ((IEChunkGenerator_AlternateDim) result).ip_setFeaturesPerStep(
            Suppliers.memoize(
                () -> FeatureSorter.buildFeaturesPerStep(
                    List.copyOf(overworldBiomeSource.possibleBiomes()),
                    holder -> {
                        Biome biome = holder.value();
                        BiomeGenerationSettings bgs = biome.getGenerationSettings();
                        List<HolderSet<PlacedFeature>> features = bgs.features();
                        // TODO modify feature
                        return features;
                    },
                    true
                )
            )
        );
        
        return result;
    }
    
    private final NoiseBasedChunkGenerator delegate;
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        ((IEChunkAccess_AlternateDim) chunkAccess).ip_setNoiseChunk(null);
        
        return delegate.fillFromNoise(
            executor, blender, delegatedRandomState, structureManager, chunkAccess
        ).thenApply(c -> {
            ((IEChunkAccess_AlternateDim) c).ip_setNoiseChunk(null);
            return c;
        });
    }
    
    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup, RandomState randomState, long seed) {
        // filter the mineshaft out
        // cannot use HolderLookup.filterElements because it does not provide id in predicate
        HolderLookup<StructureSet> structureSetLookupDelegate = new HolderLookup<StructureSet>() {
            @Override
            public Stream<Holder.Reference<StructureSet>> listElements() {
                return structureSetLookup.listElements().filter(
                    holder -> !holder.key().location().getPath().equals("mineshafts")
                );
            }
            
            @Override
            public Stream<HolderSet.Named<StructureSet>> listTags() {
                return structureSetLookup.listTags();
            }
            
            @Override
            public Optional<Holder.Reference<StructureSet>> get(ResourceKey<StructureSet> resourceKey) {
                return structureSetLookup.get(resourceKey);
            }
            
            @Override
            public Optional<HolderSet.Named<StructureSet>> get(TagKey<StructureSet> tagKey) {
                return structureSetLookup.get(tagKey);
            }
        };
        
        return super.createState(structureSetLookupDelegate, randomState, seed);
    }
}
