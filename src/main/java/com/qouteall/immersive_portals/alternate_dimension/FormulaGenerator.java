package com.qouteall.immersive_portals.alternate_dimension;

import java.util.Random;
import java.util.function.Function;

public class FormulaGenerator {
    
    public static interface TriNumFunction {
        double eval(double x, double y, double z);
    }
    
    public static interface UniNumFunction {
        double eval(double x);
    }
    
    private static RandomSelector<Function<Random, UniNumFunction>> uniFuncSelector;
    private static RandomSelector<TriNumFunction> triFuncSelector;
    
    public static void init() {
        initUniFuncSelector();
        initTriFuncSelector();
    }
    
    private static void initUniFuncSelector() {
        RandomSelector.Builder<Function<Random, UniNumFunction>> builder = new RandomSelector.Builder<>();
        
        Function<Function<Double, UniNumFunction>, Function<Random, UniNumFunction>> applyRandomArg =
            arged -> random -> arged.apply(random.nextDouble());
        Function<UniNumFunction, Function<Random, UniNumFunction>> noRandomArg =
            func -> random -> func;
        
        builder.add(30, noRandomArg.apply(x -> x));
        builder.add(30, noRandomArg.apply(x -> -x));
        builder.add(30, noRandomArg.apply(x -> x * x));
        builder.add(30, noRandomArg.apply(x -> x * x * x));
        builder.add(10, noRandomArg.apply(x -> Math.sin(x * 3)));
        builder.add(10, noRandomArg.apply(x -> Math.cos(x * 7)));
        builder.add(10, noRandomArg.apply(x -> Math.exp(x * 3)));
        builder.add(10, noRandomArg.apply(x -> Math.log(Math.abs(x) + 1)));
        builder.add(5, noRandomArg.apply(x -> Math.cosh(x)));
        builder.add(10, noRandomArg.apply(x -> Math.abs(x)));
        builder.add(5, noRandomArg.apply(x -> Math.round(x)));
        builder.add(10, noRandomArg.apply(x -> Math.sqrt(Math.abs(x))));
        builder.add(10, noRandomArg.apply(x -> weirdSwap(x)));
        builder.add(100, applyRandomArg.apply(arg -> x -> x * arg));
        builder.add(50, applyRandomArg.apply(arg -> x -> x / Math.max(arg, 0.001)));
        builder.add(10, applyRandomArg.apply(arg -> x -> Math.max(x, arg)));
        builder.add(10, applyRandomArg.apply(arg -> x -> Math.floor(x * arg * 23)));
        builder.add(5, applyRandomArg.apply(arg -> x -> weirdAnd(arg, x)));
        builder.add(10, applyRandomArg.apply(arg -> x -> weirdXor(arg, x)));
        
        uniFuncSelector = builder.build();
    }
    
    private static int toInt(double d) {
        return (int) (d * 256);
    }
    
    private static double fromInt(int d) {
        return (double) (((double) d) / 256);
    }
    
    private static double weirdAnd(double a, double b) {
        return fromInt(toInt(a) & toInt(b));
    }
    
    private static double weirdXor(double a, double b) {
        return fromInt(toInt(a) ^ toInt(b));
    }
    
    private static double weirdSwap(double a) {
        int i = toInt(a);
        
        int r = ((i & 0x0F) << 4) | ((i & 0xF0) >> 4);
        
        return fromInt(r);
    }
    
    private static void initTriFuncSelector() {
        RandomSelector.Builder<TriNumFunction> builder = new RandomSelector.Builder<>();
        
        builder.add(100, (x, y, z) -> x + y + z);
        builder.add(50, (x, y, z) -> x * y * z);
        builder.add(10, (x, y, z) -> x * y + z);
        builder.add(10, (x, y, z) -> x + y * z);
        builder.add(10, (x, y, z) -> x * z + y);
        builder.add(10, (x, y, z) -> x + y + z);
        builder.add(10, (x, y, z) -> x * x + y * y + z * z);
        builder.add(10, (x, y, z) -> x + y * y + z * z);
        builder.add(10, (x, y, z) -> x * x + y + z * z);
        builder.add(10, (x, y, z) -> x * x + y * y + z);
        builder.add(10, (x, y, z) -> -x * x + y * y + z * z);
        builder.add(10, (x, y, z) -> x * x - y * y + z * z);
        builder.add(10, (x, y, z) -> x * x + y * y - z * z);
        builder.add(3, (x, y, z) -> Math.pow(y, x + z));
        builder.add(3, (x, y, z) -> Math.pow(y, x - z));
        builder.add(3, (x, y, z) -> Math.pow(x * z, y));
        builder.add(20, (x, y, z) -> Math.log(Math.abs(x + y + z) + 0.5));
        
        triFuncSelector = builder.build();
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
    
    public static UniNumFunction limitRange(
        UniNumFunction fun
    ) {
        double v1 = fun.eval(0);
        double v3 = fun.eval(1);
        
        final double tooBigThreshold = 233.0;
        // having a too big number range may cause it to generate solid or void
        if (Math.abs(v1) > tooBigThreshold || Math.abs(v3) > tooBigThreshold) {
            return x -> Math.log(Math.abs(fun.eval(x)) + 1);
        }
        else {
            return fun;
        }
    }
    
    public static UniNumFunction addConstant(
        UniNumFunction fun
    ) {
        double v1 = fun.eval(0);
        double v2 = fun.eval(0.5);
        double v3 = fun.eval(1);
        
        double average = (v1 + v2 + v3) / 3;
        double offset = 0.5 - average;
        if (Math.abs(offset) < 0.1) {
            return fun;
        }
        
        return x -> fun.eval(x) + offset;
    }
    
    public static UniNumFunction processFunc(UniNumFunction fun) {
        return addConstant(limitRange(fun));
    }
    
    public static UniNumFunction getRandomUniExpressionOld(
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
    
    public static TriNumFunction getRandomTriExpressionOld(
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

//    public static UniNumFunction getComplexUniExpressionOld(
//        Random random,
//        int nestingLayer
//    ) {
//        if (nestingLayer == 0) {
//            return addConstant(
//                mergeExpression(
//                    getRandomTriExpressionOld(random.nextInt()),
//                    addConstant(getRandomUniExpressionOld(random.nextInt())),
//                    addConstant(getRandomUniExpressionOld(random.nextInt())),
//                    addConstant(getRandomUniExpressionOld(random.nextInt()))
//                )
//            );
//        }
//
//        return mergeExpression(
//            getRandomTriExpressionOld(random.nextInt()),
//            getComplexUniExpressionOld(random, nestingLayer - 1),
//            getComplexUniExpressionOld(random, nestingLayer - 1),
//            getComplexUniExpressionOld(random, nestingLayer - 1)
//        );
//    }

//    public static TriNumFunction getRandomTriCompositeExpressionOld(
//        Random random
//    ) {
//        return nestExpression(
//            getRandomTriExpressionOld(random.nextInt()),
//            getComplexUniExpressionOld(random, 2),
//            getComplexUniExpressionOld(random, 3),
//            getComplexUniExpressionOld(random, 2)
//        );
//    }
    
    public static UniNumFunction getComplexUniExpression(
        Random random,
        int nestingLayer
    ) {
        if (nestingLayer == 0) {
            return processFunc(
                mergeExpression(
                    triFuncSelector.select(random),
                    processFunc(uniFuncSelector.select(random).apply(random)),
                    processFunc(uniFuncSelector.select(random).apply(random)),
                    processFunc(uniFuncSelector.select(random).apply(random))
                )
            );
        }
        
        return mergeExpression(
            triFuncSelector.select(random),
            getComplexUniExpression(random, nestingLayer - 1),
            getComplexUniExpression(random, nestingLayer - 1),
            getComplexUniExpression(random, nestingLayer - 1)
        );
    }
    
    public static TriNumFunction getRandomTriCompositeExpression(
        Random random
    ) {
        return nestExpression(
            triFuncSelector.select(random),
            getComplexUniExpression(random, 2),
            getComplexUniExpression(random, 2),
            getComplexUniExpression(random, 2)
        );
    }
}
