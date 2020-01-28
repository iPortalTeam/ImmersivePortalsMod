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
    
    public static UniNumFunction addConstant(
        UniNumFunction a
    ) {
        double v1 = a.eval(0);
        double v2 = a.eval(0.5);
        double v3 = a.eval(1);
        double average = (v1 + v2 + v3) / 3;
        double sub = average - 0.5;
        if (sub == 0) {
            return a;
        }
        return x -> a.eval(x) - sub;
    }
    
    public static UniNumFunction getRandomUniExpression(
        int seed
    ) {
        int selector = seed % 80;
        
        if (selector < 10) {
            return x -> x;
        }
        else if (selector < 20) {
            int factor = (seed % 7) + 1;
            return x -> x * factor;
        }
        else if (selector < 30) {
            int factor = (seed % 7) + 1;
            return x -> x * -factor;
        }
        else if (selector < 38) {
            int divisor = (seed % 7) + 1;
            return x -> x / divisor;
        }
        else if (selector < 42) {
            return x -> x * x;
        }
        else if (selector < 48) {
            return x -> Math.sin(x * 3);
        }
        else if (selector < 56) {
            return x -> Math.cos(x * 7);
        }
        else if (selector < 60) {
            return Math::exp;
        }
        else if (selector < 65) {
            return x -> Math.sqrt(Math.abs(x));
        }
        else if (selector < 70) {
            return x -> Math.max(x, 0.5);
        }
        else if (selector < 76) {
            return x -> Math.floor(x * 12);
        }
        else {
            return x -> x * x * x;
        }
    }
    
    public static TriNumFunction getRandomTriExpression(
        int seed
    ) {
        int selector = seed % 90;
        
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
            return (x, y, z) -> x * x - y * y + z * z;
        }
        else if (selector < 55) {
            return (x, y, z) -> x * x + y * y + z * z;
        }
        else if (selector < 60) {
            return (x, y, z) -> x + y * z;
        }
        else if (selector < 67) {
            return (x, y, z) -> x - y * z;
        }
        else if (selector < 75) {
            return (x, y, z) -> x * z + y * 2;
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
            return addConstant(
                mergeExpression(
                    getRandomTriExpression(random.nextInt()),
                    addConstant(getRandomUniExpression(random.nextInt())),
                    addConstant(getRandomUniExpression(random.nextInt())),
                    addConstant(getRandomUniExpression(random.nextInt()))
                )
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
            getComplexUniExpression(random, 2),
            getComplexUniExpression(random, 3),
            getComplexUniExpression(random, 2)
        );
    }
}
