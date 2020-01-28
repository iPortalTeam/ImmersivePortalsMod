package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.biome.source.SeedMixer;

import java.util.Arrays;
import java.util.Random;

class RegionErrorTerrainGenerator {
    private int regionX;
    private int regionZ;
    private FormulaGenerator.TriNumFunction expression;
    private double b1;
    private double b2;
    private double middle;
    private int compositionType;
    
    public RegionErrorTerrainGenerator(
        int regionX_,
        int regionZ_,
        long seed
    ) {
        regionX = regionX_;
        regionZ = regionZ_;
        
        initExpression(seed);
        
        calculateMiddle();
    }
    
    private void initExpression(long seed) {
        long realSeed = SeedMixer.mixSeed(
            seed, SeedMixer.mixSeed(
                regionX, regionZ
            )
        );
        Random random = new Random(realSeed);
        expression = FormulaGenerator.getRandomTriCompositeExpression(random);
        
        compositionType = (int) (realSeed % 5);
    }
    
    private void calculateMiddle() {
        double[] arr = new double[8];
        double b = 0.4;
        double t = 0.5;
        arr[0] = expression.eval(0, b, 0);
        arr[1] = expression.eval(0, b, 1);
        arr[2] = expression.eval(0, t, 0);
        arr[3] = expression.eval(0, t, 1);
        arr[4] = expression.eval(1, b, 0);
        arr[5] = expression.eval(1, b, 1);
        arr[6] = expression.eval(1, t, 0);
        arr[7] = expression.eval(1, t, 1);
        Arrays.sort(arr);
        
        middle = arr[4];
    }
    
    private double calc(int worldX, int worldY, int worldZ) {
        int a = ErrorTerrainGenerator.regionChunkNum * 16;
        
        int regionStartX = regionX * a;
        int regionStartZ = regionZ * a;
        return expression.eval(
            (worldX - regionStartX) / ((double) a),
            worldY / ((double) ErrorTerrainGenerator.maxY),
            (worldZ - regionStartZ) / ((double) a)
        );
    }
    
    public ErrorTerrainGenerator.BlockComposition getBlockComposition(
        ErrorTerrainGenerator.TaskInfo taskInfo,
        int worldX,
        int worldY,
        int worldZ
    ) {
        if (worldY >= ErrorTerrainGenerator.maxY) {
            return ErrorTerrainGenerator.BlockComposition.air;
        }
        
        double currValue = calc(worldX, worldY, worldZ);
        
        switch (compositionType) {
            case 0:
            case 1:
            case 2:
                return solidType(worldY, currValue);
            case 3:
                return hollowType(worldY, currValue);
            case 4:
            default:
                return wateryType(worldY, currValue);
        }
    }
    
    private ErrorTerrainGenerator.BlockComposition solidType(int worldY, double currValue) {
        double splitPoint = middle;
        splitPoint *= Math.exp(Math.abs(worldY - ErrorTerrainGenerator.averageY) / 32.0);
        
        splitPoint *= Math.max(
            0.7,
            30.0 / Math.max(1, Math.min(worldY, ErrorTerrainGenerator.maxY - worldY))
        );
        
        if (currValue > splitPoint) {
            return ErrorTerrainGenerator.BlockComposition.stone;
        }
        else {
            return ErrorTerrainGenerator.BlockComposition.air;
        }
    }
    
    private ErrorTerrainGenerator.BlockComposition hollowType(int worldY, double currValue) {
        double splitPoint = middle;
        splitPoint *= Math.exp(Math.abs(worldY - ErrorTerrainGenerator.averageY) / 32.0);
        
        splitPoint *= Math.max(
            0.7,
            30.0 / Math.max(1, Math.min(worldY, ErrorTerrainGenerator.maxY - worldY))
        );
        
        if (currValue > splitPoint) {
            if (currValue > splitPoint + 2) {
                return ErrorTerrainGenerator.BlockComposition.air;
            }
            else {
                return ErrorTerrainGenerator.BlockComposition.stone;
            }
        }
        else {
            return ErrorTerrainGenerator.BlockComposition.air;
        }
    }
    
    private ErrorTerrainGenerator.BlockComposition wateryType(int worldY, double currValue) {
        double splitPoint = middle;
        splitPoint *= Math.exp(Math.abs(worldY - ErrorTerrainGenerator.averageY) / 16.0);
        
        splitPoint *= Math.max(
            0.7,
            30.0 / Math.max(1, Math.min(worldY, ErrorTerrainGenerator.maxY - worldY))
        );
        
        if (currValue > splitPoint) {
            if (((int) currValue) % 23 == 0) {
                return ErrorTerrainGenerator.BlockComposition.water;
            }
            else {
                return ErrorTerrainGenerator.BlockComposition.stone;
            }
        }
        else {
            return ErrorTerrainGenerator.BlockComposition.air;
        }
    }
}
