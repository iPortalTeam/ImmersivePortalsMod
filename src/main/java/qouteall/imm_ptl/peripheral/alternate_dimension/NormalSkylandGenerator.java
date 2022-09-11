package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelStorageSource;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunkAccess_AlternateDim;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseRouterData;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * It extends NoiseBasedChunkGenerator because in
 * {@link ChunkMap#ChunkMap(ServerLevel, LevelStorageSource.LevelStorageAccess, DataFixer, StructureTemplateManager, Executor, BlockableEventLoop, LightChunkGetter, ChunkGenerator, ChunkProgressListener, ChunkStatusUpdateListener, Supplier, int, boolean)}
 * It uses instanceof to initialize random source.
 */
public class NormalSkylandGenerator extends NoiseBasedChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(
        instance -> instance.group(
                RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter(g -> g.structureSets),
                RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(g -> g.biomeRegistry),
                RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter(g -> g.noiseRegistry),
                RegistryOps.retrieveRegistry(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).forGetter(g -> g.noiseGeneratorSettingsRegistry),
                Codec.LONG.optionalFieldOf("seed", 0L).forGetter(g -> g.seed)
            )
            .apply(instance, NormalSkylandGenerator::create)
    );
    
    private RandomState delegatedRandomState;
    
    private final long seed;
    
    public NormalSkylandGenerator(
        Registry<StructureSet> structureSets,
        Registry<NormalNoise.NoiseParameters> noiseParametersRegistry,
        BiomeSource biomeSource,
        Holder<NoiseGeneratorSettings> noiseGeneratorSettings,
        
        Registry<Biome> biomeRegistry,
        Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry,
        
        NoiseBasedChunkGenerator delegate,
        NoiseGeneratorSettings delegateNGS,
        
        long seed
    ) {
        super(structureSets, noiseParametersRegistry, biomeSource, noiseGeneratorSettings);
        
        this.biomeRegistry = biomeRegistry;
        this.noiseRegistry = noiseParametersRegistry;
        this.noiseGeneratorSettingsRegistry = noiseGeneratorSettingsRegistry;
        this.delegate = delegate;
        this.delegateNGS = delegateNGS;
        this.seed = seed;
        
        this.delegatedRandomState = RandomState.create(delegateNGS, noiseParametersRegistry, seed);
    }
    
    public static NormalSkylandGenerator create(
        Registry<StructureSet> structureSets, Registry<Biome> biomeRegistry,
        Registry<NormalNoise.NoiseParameters> noiseRegistry,
        Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry,
        long seed
    ) {
        MultiNoiseBiomeSource overworldBiomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(biomeRegistry);
        Set<Holder<Biome>> overworldBiomes = overworldBiomeSource.possibleBiomes();
        
        BiomeSource chaosBiomeSource = new ChaosBiomeSource(HolderSet.direct(new ArrayList<>(overworldBiomes)));
        
        NoiseGeneratorSettings overworldNGS = IENoiseGeneratorSettings.ip_overworld(false, false);
        
        NoiseGeneratorSettings intrinsicSkylandNGS = IENoiseGeneratorSettings.ip_floatingIslands();
        
        NoiseGeneratorSettings endNGS = IENoiseGeneratorSettings.ip_end();
        
        NoiseGeneratorSettings usedSkylandNGS = new NoiseGeneratorSettings(
            intrinsicSkylandNGS.noiseSettings(),
            intrinsicSkylandNGS.defaultBlock(),
            intrinsicSkylandNGS.defaultFluid(),
            IENoiseRouterData.ip_noNewCaves(
                BuiltinRegistries.DENSITY_FUNCTION,
                IENoiseRouterData.ip_slideEndLike(IENoiseRouterData.ip_getFunction(BuiltinRegistries.DENSITY_FUNCTION, IENoiseRouterData.get_BASE_3D_NOISE_END()), 0, 128)
            ),
            intrinsicSkylandNGS.surfaceRule(),
            intrinsicSkylandNGS.spawnTarget(),
            intrinsicSkylandNGS.seaLevel(),
            intrinsicSkylandNGS.disableMobGeneration(),
            intrinsicSkylandNGS.aquifersEnabled(),
            intrinsicSkylandNGS.oreVeinsEnabled(),
            intrinsicSkylandNGS.useLegacyRandomSource()
        );
        
        NoiseBasedChunkGenerator overworldGenerator = new NoiseBasedChunkGenerator(
            structureSets, noiseRegistry,
            overworldBiomeSource, Holder.direct(overworldNGS)
        );
        
        return new NormalSkylandGenerator(
            structureSets,
            noiseRegistry,
            overworldBiomeSource,
            Holder.direct(usedSkylandNGS),
            biomeRegistry,
            noiseGeneratorSettingsRegistry,
            overworldGenerator,
            overworldNGS,
            seed
        );
    }
    
    public final Registry<Biome> biomeRegistry;
    public final Registry<NormalNoise.NoiseParameters> noiseRegistry;
    public final Registry<NoiseGeneratorSettings> noiseGeneratorSettingsRegistry;
    private final NoiseBasedChunkGenerator delegate;
    private final NoiseGeneratorSettings delegateNGS;
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        CompletableFuture<ChunkAccess> original = super.fillFromNoise(executor, blender, randomState, structureManager, chunkAccess);
        return original.thenComposeAsync(chunkAccess1 -> {
            ((IEChunkAccess_AlternateDim) chunkAccess1).ip_setNoiseChunk(null);
            return delegate.createBiomes(biomeRegistry, executor, delegatedRandomState, blender, structureManager, chunkAccess1);
        });
    }
}
