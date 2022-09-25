package qouteall.imm_ptl.core.portal.animation;

import org.jetbrains.annotations.NotNull;

public enum TimingFunction {
    linear, sine, circle;
    
    @NotNull
    static TimingFunction fromString(String c) {
        TimingFunction timingFunction = switch (c) {
            case "linear" -> linear;
            case "sine" -> sine;
            case "circle" -> circle;
            default -> sine;
        };
        return timingFunction;
    }
    
    public double mapProgress(double progress) {
        switch (this) {
            
            case linear -> {return progress;}
            case sine -> {return Math.sin(progress * (Math.PI / 2));}
            case circle -> {return Math.sqrt(1 - (1 - progress) * (1 - progress));}
        }
        throw new RuntimeException();
    }
}
