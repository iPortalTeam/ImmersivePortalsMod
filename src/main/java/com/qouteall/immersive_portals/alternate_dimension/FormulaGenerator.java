package com.qouteall.immersive_portals.alternate_dimension;

import java.util.Random;

public class FormulaGenerator {
    public static interface TriNumFunction {
        double eval(double x, double y, double z);
    }
    
    public static interface BiNumFunction {
        double eval(double x, double y);
    }
    
    public static interface UniNumFunction {
        double eval(double x);
    }
    
    public static UniNumFunction nestExpression(
        UniNumFunction inner,
        UniNumFunction outer
    ) {
        return x -> outer.eval(inner.eval(x));
    }
    
    public static TriNumFunction nestExpression(
        TriNumFunction base,
        UniNumFunction a,
        UniNumFunction b,
        UniNumFunction c
    ) {
        return (x, y, z) -> base.eval(a.eval(x), b.eval(y), c.eval(z));
    }
    
    public static UniNumFunction mergeExpression(
        TriNumFunction base,
        UniNumFunction a,
        UniNumFunction b,
        UniNumFunction c
    ) {
        return x -> base.eval(a.eval(x), b.eval(x), c.eval(x));
    }
    
    public static UniNumFunction getRandomUniExpression(
        int seed
    ) {
        int selector = seed % 77;
        
        if (selector < 5) {
            int offset = seed % 73;
            return x -> x + offset;
        }
        else if (selector < 14) {
            int offset = seed % 47;
            return x -> x - offset;
        }
        else if (selector < 21) {
            int factor = (seed % 6) + 1;
            return x -> x * factor;
        }
        else if (selector < 28) {
            int factor = (seed % 6) + 1;
            return x -> x * -factor;
        }
        else if (selector < 34) {
            int factor = (seed % 6) + 1;
            return x -> x * -factor;
        }
        else if (selector < 37) {
            int divisor = (seed % 6) + 1;
            return x -> x / divisor;
        }
        else if (selector < 40) {
            return x -> x * x;
        }
        else if (selector < 48) {
            return x -> Math.sin(x / 20.0);
        }
        else if (selector < 56) {
            return x -> Math.cos(x / 30.0);
        }
        else if (selector < 60) {
            return Math::exp;
        }
        else if (seed < 65) {
            return x -> Math.sqrt(Math.abs(x));
        }
        else {
            return x -> x;
        }
    }
    
    public static TriNumFunction getRandomTriExpression(
        int seed
    ) {
        int selector = seed % 87;
        
        if (selector < 30) {
            return (x, y, z) -> x + y + z;
        }
        else if (selector < 37) {
            return (x, y, z) -> x - y + z;
        }
        else if (selector < 45) {
            return (x, y, z) -> x * y - z;
        }
        else if (selector < 50) {
            return (x, y, z) -> x * x - y * y + z;
        }
        else if (selector < 55) {
            return (x, y, z) -> x * x + y * y + z * z;
        }
        else if (selector < 60) {
            return (x, y, z) -> x + y * z;
        }
        else if (selector < 65) {
            return (x, y, z) -> x * y * z;
        }
        else if (selector < 70) {
            return (x, y, z) -> x - y * z;
        }
        else if (selector < 80) {
            return (x, y, z) -> x * z - y;
        }
        else {
            return (x, y, z) -> x * y * z;
        }
    }
    
    public static UniNumFunction getComplexUniExpression(
        Random random,
        int nestingLayer
    ) {
        if (nestingLayer == 0) {
            return mergeExpression(
                getRandomTriExpression(random.nextInt()),
                getRandomUniExpression(random.nextInt()),
                getRandomUniExpression(random.nextInt()),
                getRandomUniExpression(random.nextInt())
            );
        }
        
        return mergeExpression(
            getRandomTriExpression(random.nextInt()),
            getComplexUniExpression(random, nestingLayer - 1),
            getComplexUniExpression(random, nestingLayer - 1),
            getComplexUniExpression(random, nestingLayer - 1)
        );
    }
    
    public static TriNumFunction getRandomTriCompositeExpression(
        Random random
    ) {
        return nestExpression(
            getRandomTriExpression(random.nextInt()),
            getComplexUniExpression(random, 0),
            getComplexUniExpression(random, 0),
            getComplexUniExpression(random, 0)
        );
    }
}
