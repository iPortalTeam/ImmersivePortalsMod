package com.qouteall.immersive_portals.alternate_dimension;

import com.qouteall.immersive_portals.ducks.IESurfaceChunkGenerator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGenerator;
import net.minecraft.world.gen.chunk.FloatingIslandsChunkGeneratorConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.Iterator;

public class MyFloatingIslandChunkGenerator extends FloatingIslandsChunkGenerator {
    private final BlockState AIR;
    
    public MyFloatingIslandChunkGenerator(
        IWorld iWorld,
        BiomeSource biomeSource,
        FloatingIslandsChunkGeneratorConfig floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
        AIR = Blocks.AIR.getDefaultState();
    }
    
    @Override
    public void populateNoise(IWorld world, Chunk chunk) {
        
        IESurfaceChunkGenerator ie = (IESurfaceChunkGenerator) this;
        
        int verticalNoiseResolution_ = ie.get_verticalNoiseResolution();
        int horizontalNoiseResolution_ = ie.get_horizontalNoiseResolution();
        int noiseSizeX_ = ie.get_noiseSizeX();
        int noiseSizeY_ = ie.get_noiseSizeY();
        int noiseSizeZ_ = ie.get_noiseSizeZ();
        
        int i = getSeaLevel();
        ObjectList<PoolStructurePiece> objectList = new ObjectArrayList(10);
        ObjectList<JigsawJunction> objectList2 = new ObjectArrayList(32);
        ChunkPos chunkPos = chunk.getPos();
        int j = chunkPos.x;
        int k = chunkPos.z;
        int l = j << 4;
        int m = k << 4;
        Iterator var11 = Feature.JIGSAW_STRUCTURES.iterator();
        
        label178:
        while (var11.hasNext()) {
            StructureFeature<?> structureFeature = (StructureFeature) var11.next();
            String string = structureFeature.getName();
            LongIterator longIterator = chunk.getStructureReferences(string).iterator();
            
            label176:
            while (true) {
                StructureStart structureStart;
                do {
                    do {
                        if (!longIterator.hasNext()) {
                            continue label178;
                        }
                        
                        long n = longIterator.nextLong();
                        ChunkPos chunkPos2 = new ChunkPos(n);
                        Chunk chunk2 = world.getChunk(chunkPos2.x, chunkPos2.z);
                        structureStart = chunk2.getStructureStart(string);
                    } while (structureStart == null);
                } while (!structureStart.hasChildren());
                
                Iterator var20 = structureStart.getChildren().iterator();
                
                while (true) {
                    StructurePiece structurePiece;
                    do {
                        do {
                            if (!var20.hasNext()) {
                                continue label176;
                            }
                            
                            structurePiece = (StructurePiece) var20.next();
                        } while (!structurePiece.method_16654(chunkPos, 12));
                    } while (!(structurePiece instanceof PoolStructurePiece));
                    
                    PoolStructurePiece poolStructurePiece = (PoolStructurePiece) structurePiece;
                    StructurePool.Projection projection = poolStructurePiece.getPoolElement().getProjection();
                    if (projection == StructurePool.Projection.RIGID) {
                        objectList.add(poolStructurePiece);
                    }
                    
                    Iterator var24 = poolStructurePiece.getJunctions().iterator();
                    
                    while (var24.hasNext()) {
                        JigsawJunction jigsawJunction = (JigsawJunction) var24.next();
                        int o = jigsawJunction.getSourceX();
                        int p = jigsawJunction.getSourceZ();
                        if (o > l - 12 && p > m - 12 && o < l + 15 + 12 && p < m + 15 + 12) {
                            objectList2.add(jigsawJunction);
                        }
                    }
                }
            }
        }
        
        double[][][] ds = new double[2][noiseSizeZ_ + 1][noiseSizeY_ + 1];
        
        for (int q = 0; q < noiseSizeZ_ + 1; ++q) {
            ds[0][q] = new double[noiseSizeY_ + 1];
            sampleNoiseColumn(ds[0][q], j * noiseSizeX_, k * noiseSizeZ_ + q);
            ds[1][q] = new double[noiseSizeY_ + 1];
        }
        
        ProtoChunk protoChunk = (ProtoChunk) chunk;
        Heightmap heightmap = protoChunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = protoChunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        ObjectListIterator<PoolStructurePiece> objectListIterator = objectList.iterator();
        ObjectListIterator<JigsawJunction> objectListIterator2 = objectList2.iterator();
        
        for (int r = 0; r < noiseSizeX_; ++r) {
            int t;
            for (t = 0; t < noiseSizeZ_ + 1; ++t) {
                sampleNoiseColumn(
                    ds[1][t],
                    j * noiseSizeX_ + r + 1,
                    k * noiseSizeZ_ + t
                );
            }
            
            for (t = 0; t < noiseSizeZ_; ++t) {
                ChunkSection chunkSection = protoChunk.getSection(15);
                chunkSection.lock();
                
                for (int u = noiseSizeY_ - 1; u >= 0; --u) {
                    double d = ds[0][t][u];
                    double e = ds[0][t + 1][u];
                    double f = ds[1][t][u];
                    double g = ds[1][t + 1][u];
                    double h = ds[0][t][u + 1];
                    double v = ds[0][t + 1][u + 1];
                    double w = ds[1][t][u + 1];
                    double x = ds[1][t + 1][u + 1];
                    
                    for (int y = verticalNoiseResolution_ - 1; y >= 0; --y) {
                        int z = u * verticalNoiseResolution_ + y;
                        int aa = z & 15;
                        int ab = z >> 4;
                        if (chunkSection.getYOffset() >> 4 != ab) {
                            chunkSection.unlock();
                            chunkSection = protoChunk.getSection(ab);
                            chunkSection.lock();
                        }
                        
                        double ac = (double) y / (double) verticalNoiseResolution_;
                        double ad = MathHelper.lerp(ac, d, h);
                        double ae = MathHelper.lerp(ac, f, w);
                        double af = MathHelper.lerp(ac, e, v);
                        double ag = MathHelper.lerp(ac, g, x);
                        
                        for (int ah = 0; ah < horizontalNoiseResolution_; ++ah) {
                            int ai = l + r * horizontalNoiseResolution_ + ah;
                            int aj = ai & 15;
                            double ak = (double) ah / (double) horizontalNoiseResolution_;
                            double al = MathHelper.lerp(ak, ad, ae);
                            double am = MathHelper.lerp(ak, af, ag);
                            
                            for (int an = 0; an < horizontalNoiseResolution_; ++an) {
                                int ao = m + t * horizontalNoiseResolution_ + an;
                                int ap = ao & 15;
                                double aq = (double) an / (double) horizontalNoiseResolution_;
                                double ar = MathHelper.lerp(aq, al, am);
                                double as = MathHelper.clamp(ar / 200.0D, -1.0D, 1.0D);
                                
                                int ax;
                                int ay;
                                int av;
                                for (as = as / 2.0D - as * as * as / 24.0D; objectListIterator.hasNext(); as += method_16572(
                                    ax,
                                    ay,
                                    av
                                ) * 0.8D) {
                                    PoolStructurePiece poolStructurePiece2 = (PoolStructurePiece) objectListIterator.next();
                                    BlockBox blockBox = poolStructurePiece2.getBoundingBox();
                                    ax = Math.max(
                                        0,
                                        Math.max(blockBox.minX - ai, ai - blockBox.maxX)
                                    );
                                    ay = z - (blockBox.minY + poolStructurePiece2.getGroundLevelDelta());
                                    av = Math.max(
                                        0,
                                        Math.max(blockBox.minZ - ao, ao - blockBox.maxZ)
                                    );
                                }
                                
                                objectListIterator.back(objectList.size());
                                
                                while (objectListIterator2.hasNext()) {
                                    JigsawJunction jigsawJunction2 = (JigsawJunction) objectListIterator2.next();
                                    int aw = ai - jigsawJunction2.getSourceX();
                                    ax = z - jigsawJunction2.getSourceGroundY();
                                    ay = ao - jigsawJunction2.getSourceZ();
                                    as += method_16572(aw, ax, ay) * 0.4D;
                                }
                                
                                objectListIterator2.back(objectList2.size());
                                BlockState blockState3;
                                if (as > 0.0D) {
                                    blockState3 = defaultBlock;
                                }
                                else if (z < i) {
                                    blockState3 = defaultFluid;
                                }
                                else {
                                    blockState3 = AIR;
                                }
                                
                                if (blockState3 != AIR) {
                                    if (blockState3.getLuminance() != 0) {
                                        mutable.set(ai, z, ao);
                                        protoChunk.addLightSource(mutable);
                                    }
                                    
                                    chunkSection.setBlockState(aj, aa, ap, blockState3, false);
                                    heightmap.trackUpdate(aj, z, ap, blockState3);
                                    heightmap2.trackUpdate(aj, z, ap, blockState3);
                                }
                            }
                        }
                    }
                }
                
                chunkSection.unlock();
            }
            
            double[][] es = ds[0];
            ds[0] = ds[1];
            ds[1] = es;
        }
        
    }
    
    private static double method_16572(int i, int j, int k) {
        int l = i + 12;
        int m = j + 12;
        int n = k + 12;
        if (l >= 0 && l < 24) {
            if (m >= 0 && m < 24) {
                return n >= 0 && n < 24 ? (double) field_16649[n * 24 * 24 + l * 24 + m] : 0.0D;
            }
            else {
                return 0.0D;
            }
        }
        else {
            return 0.0D;
        }
    }
    
    private static final float[] field_16649 = (float[]) Util.make(new float[13824], (fs) -> {
        for (int i = 0; i < 24; ++i) {
            for (int j = 0; j < 24; ++j) {
                for (int k = 0; k < 24; ++k) {
                    fs[i * 24 * 24 + j * 24 + k] = (float) method_16571(j - 12, k - 12, i - 12);
                }
            }
        }
        
    });
    
    private static double method_16571(int i, int j, int k) {
        double d = (double) (i * i + k * k);
        double e = (double) j + 0.5D;
        double f = e * e;
        double g = Math.pow(2.718281828459045D, -(f / 16.0D + d / 16.0D));
        double h = -e * MathHelper.fastInverseSqrt(f / 2.0D + d / 2.0D) / 2.0D;
        return h * g;
    }
}
