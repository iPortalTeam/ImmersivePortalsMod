package com.qouteall.immersive_portals.nether_portal_managing;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import javafx.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ObsidianFrame {
    
    public final Direction.Axis normalAxis;
    
    //this box does not contain obsidian frame
    public final IntegerAABBInclusive boxWithoutObsidian;
    
    public ObsidianFrame(
        Direction.Axis normalAxis,
        IntegerAABBInclusive boxWithoutObsidian
    ) {
        this.normalAxis = normalAxis;
        this.boxWithoutObsidian = boxWithoutObsidian;
    }
    
    public static IntegerAABBInclusive expandToIncludeObsidianBlocks(
        Direction.Axis axisOfNormal,
        IntegerAABBInclusive boxInsideObsidianFrame
    ) {
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(
            axisOfNormal
        );
        
        return boxInsideObsidianFrame
            .getExpanded(anotherTwoAxis.getKey(), 1)
            .getExpanded(anotherTwoAxis.getValue(), 1);
    }
    
    public static IntegerAABBInclusive shrinkToExcludeObsidianBlocks(
        Direction.Axis axisOfNormal,
        IntegerAABBInclusive boxInsideObsidianFrame
    ) {
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(
            axisOfNormal
        );
        
        return boxInsideObsidianFrame
            .getExpanded(anotherTwoAxis.getKey(), -1)
            .getExpanded(anotherTwoAxis.getValue(), -1);
    }
}
