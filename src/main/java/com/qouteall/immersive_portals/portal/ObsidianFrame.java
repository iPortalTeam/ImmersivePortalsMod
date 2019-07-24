package com.qouteall.immersive_portals.portal;

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
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("normalAxis", normalAxis.ordinal());
        tag.putInt("boxXL", boxWithoutObsidian.l.getX());
        tag.putInt("boxYL", boxWithoutObsidian.l.getY());
        tag.putInt("boxZL", boxWithoutObsidian.l.getZ());
        tag.putInt("boxXH", boxWithoutObsidian.h.getX());
        tag.putInt("boxYH", boxWithoutObsidian.h.getY());
        tag.putInt("boxZH", boxWithoutObsidian.h.getZ());
        return tag;
    }
    
    public static ObsidianFrame fromTag(CompoundTag tag) {
        return new ObsidianFrame(
            Direction.Axis.values()[tag.getInt("normalAxis")],
            new IntegerAABBInclusive(
                new BlockPos(
                    tag.getInt("boxXL"),
                    tag.getInt("boxYL"),
                    tag.getInt("boxZL")
                ),
                new BlockPos(
                    tag.getInt("boxXH"),
                    tag.getInt("boxYH"),
                    tag.getInt("boxZH")
                )
            )
        );
    }
}
