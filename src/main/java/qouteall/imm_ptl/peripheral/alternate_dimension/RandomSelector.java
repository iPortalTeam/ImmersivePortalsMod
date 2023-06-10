package qouteall.imm_ptl.peripheral.alternate_dimension;

import net.minecraft.util.Tuple;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomSelector<T> {
    private Object[] entries;
    private int[] subWeightSum;
    private int weightSum;
    
    public RandomSelector(List<Tuple<T, Integer>> data) {
        entries = data.stream().map(Tuple::getA).toArray();
        
        subWeightSum = Helper.mapReduce(
            data.stream(),
            (preSum, curr) -> preSum + curr.getB(),
            new Helper.SimpleBox<>((Integer) 0)
        ).mapToInt(i -> i).toArray();
        
        weightSum = subWeightSum[subWeightSum.length - 1];
    }
    
    public T select(Random random) {
        int randomValue = random.nextInt(weightSum);
        
        return selectByRandomValue(randomValue);
    }
    
    public T selectByRandomValue(int randomValue) {
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
        private ArrayList<Tuple<A, Integer>> data = new ArrayList<>();
        
        public Builder() {
        }
        
        public Builder<A> add(int weight, A element) {
            data.add(new Tuple<>(element, weight));
            return this;
        }
        
        public RandomSelector<A> build() {
            return new RandomSelector<>(data);
        }
    }
    
}
