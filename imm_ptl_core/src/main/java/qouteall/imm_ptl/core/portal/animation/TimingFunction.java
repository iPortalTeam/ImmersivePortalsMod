package qouteall.imm_ptl.core.portal.animation;

import org.jetbrains.annotations.NotNull;

public enum TimingFunction {
    linear, sine, circle, easeInOutCubic; // don't rename
    
    @NotNull
    static TimingFunction fromString(String c) {
        TimingFunction timingFunction = switch (c) {
            case "linear" -> linear;
            case "sine" -> sine;
            case "circle" -> circle;
            case "easeInOutCubic" -> easeInOutCubic;
            default -> sine;
        };
        return timingFunction;
    }
    
    public double mapProgress(double progress) {
        switch (this) {
            case linear -> {return progress;}
            case sine -> {return Math.sin(progress * (Math.PI / 2));} // ending derivative is 0
            case circle -> {return Math.sqrt(1 - (1 - progress) * (1 - progress));} // ending derivative is 0
            case easeInOutCubic -> {
                return -2 * (progress * progress * progress)
                    + 3* (progress * progress); // starting and ending derivatives are 0
            }
        }
        throw new RuntimeException();
    }
}
