package com.qouteall.immersive_portals.far_scenery;

import com.qouteall.immersive_portals.Helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;

//it's not the optimal equation solver
public class HeuristicChaosEquationSolver {
    
    public static class ReferencePoint {
        public final double input;
        public final double output;
        public final int generation;
        
        public ReferencePoint(double input, double output, int generation) {
            this.input = input;
            this.output = output;
            this.generation = generation;
        }
    }
    
    private ArrayList<ReferencePoint> history = new ArrayList<>();
    private int empiricalReserve;
    private int generation = 0;
    
    public HeuristicChaosEquationSolver(
        int empiricalReserve_
    ) {
        empiricalReserve = empiricalReserve_;
    }
    
    public void addPriorKnowledge(
        double independentVariable,
        double dependentVariable
    ) {
        history.add(
            new ReferencePoint(independentVariable, dependentVariable, 0x7FFFFFFF)
        );
    }
    
    public double feedNewDataAndGetNewRoot(
        double independentVariable,
        double dependentVariable
    ) {
        history.add(
            new ReferencePoint(independentVariable, dependentVariable, generation)
        );
        generation += 1;
        removeOldReference();
        
        history.sort(Comparator.comparingDouble(point -> point.input));
        
        return Helper.wrapAdjacentAndMap(
            history.stream(),
            (BiFunction<ReferencePoint, ReferencePoint, Double>) (a, b) -> {
                if (Math.abs(a.output) < 0.1) {
                    return a.input;
                }
                if (Math.abs(b.output) < 0.1) {
                    return b.input;
                }
                if (a.output * b.output < 0) {
                    if (b.output == a.output) {
                        return (a.input + b.input) / 2;
                    }
                    //linear interpolation
                    return (a.input * b.output - a.output * b.input) / (b.output - a.output);
                }
                return null;
            }
        ).filter(
            Objects::nonNull
        ).min(
            Comparator.comparingDouble(
                root -> Math.abs(root - independentVariable)
            )
        ).orElseGet(
            () -> history.stream().min(
                Comparator.comparingDouble(
                    point -> Math.abs(point.output)
                )
            ).map(
                point -> point.input
            ).orElseGet(
                () -> new Random().nextDouble()
            )
        );
    }
    
    private void removeOldReference() {
        history.removeIf(referencePoint -> referencePoint.generation < generation - empiricalReserve);
    }
}
