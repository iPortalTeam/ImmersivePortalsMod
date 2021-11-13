package qouteall.imm_ptl.peripheral.alternate_dimension;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.gen.random.ChunkRandom;
import java.util.stream.IntStream;

public class CompositeSimplexNoiseSampler {
    private SimplexNoiseSampler[] samplers;
    
    public CompositeSimplexNoiseSampler(int samplerNum, long seed) {
        // TODO recover
//        samplers = IntStream.range(0, samplerNum)
//            .mapToObj(i -> new SimplexNoiseSampler(new ChunkRandom(seed + i)))
//            .toArray(SimplexNoiseSampler[]::new);
    }
    
    public int getSamplerNum() {
        return samplers.length;
    }
    
    public int sample(double x, double z) {
        int result = 0;
        for (int i = 0; i < getSamplerNum(); i++) {
            result = result * 2;
            double r = samplers[i].sample(x, z);
            if (r > 0) {
                result += 1;
            }
        }
        return result;
    }
}
