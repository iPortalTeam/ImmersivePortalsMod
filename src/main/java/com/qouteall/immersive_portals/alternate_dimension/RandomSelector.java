package com.qouteall.immersive_portals.alternate_dimension;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomSelector<T> {
    private Object[] entries;
    private int[] subWeightSum;
    private int weightSum;
    
    public RandomSelector(List<Pair<T, Integer>> data) {
        entries = data.stream().map(Pair::getLeft).toArray();
        
        subWeightSum = Helper.mapReduce(
            data.stream(),
            (preSum, curr) -> preSum + curr.getRight(),
            new Helper.SimpleBox<>((Integer) 0)
        ).mapToInt(i -> i).toArray();
        
        weightSum = subWeightSum[subWeightSum.length - 1];
    }
    
    public T select(Random random) {
        int randomValue = random.nextInt() % weightSum;
        
        return selectByRandomValue(randomValue);
    }
    
    private T selectByRandomValue(int randomValue) {
        int result = Arrays.binarySearch(
            subWeightSum,
            0, subWeightSum.length,
            randomValue
        );
        
        if (result >= 0) {
            return (T) entries[result + 1];
        }
        else {
            //result = -firstEleGreaterThanValue - 1
            int firstEleGreaterThanValue = -(result + 1);
            return (T) entries[firstEleGreaterThanValue];
        }
    }
    
    public static class Builder<A> {
        private ArrayList<Pair<A, Integer>> data = new ArrayList<>();
        
        public Builder() {
        }
        
        public void add(int weight, A element) {
            data.add(new Pair<>(element, weight));
        }
        
        public RandomSelector<A> build() {
            return new RandomSelector<>(data);
        }
    }
    
}
