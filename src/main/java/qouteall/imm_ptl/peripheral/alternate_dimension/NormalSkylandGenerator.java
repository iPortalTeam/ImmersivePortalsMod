package qouteall.imm_ptl.peripheral.alternate_dimension;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelStorageSource;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IEChunkAccess_AlternateDim;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseGeneratorSettings;
import qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension.IENoiseRouterData;
import qouteall.q_misc_util.MiscHelper;

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
                RegistryOps.retrieveGetter(Registries.BIOME),
                RegistryOps.retrieveGetter(Registries.DENSITY_FUNCTION),
                RegistryOps.retrieveGetter(Registries.NOISE),
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
        long seed
    ) {
        MultiNoiseBiomeSource overworldBiomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(biomeHolderGetter);
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
                densityFunctionHolderGetter,
                noiseParametersHolderGetter,
                IENoiseRouterData.ip_slideEndLike(IENoiseRouterData.ip_getFunction(
                    densityFunctionHolderGetter, IENoiseRouterData.get_BASE_3D_NOISE_END()
                ), 0, 128)
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
            overworldBiomeSource, Holder.direct(overworldNGS)
        );
        
        return new NormalSkylandGenerator(
            overworldBiomeSource,
            Holder.direct(usedSkylandNGS),
            overworldGenerator,
            biomeHolderGetter,
            densityFunctionHolderGetter,
            noiseParametersHolderGetter,
            seed
        );
    }
    
    private final NoiseBasedChunkGenerator delegate;
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return codec;
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess) {
        CompletableFuture<ChunkAccess> original = super.fillFromNoise(executor, blender, randomState, structureManager, chunkAccess);
        return original.thenComposeAsync(chunkAccess1 -> {
            ((IEChunkAccess_AlternateDim) chunkAccess1).ip_setNoiseChunk(null);
            return delegate.createBiomes(executor, delegatedRandomState, blender, structureManager, chunkAccess1);
        });
    }
}
