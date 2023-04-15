package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.dimension.DimId;

import javax.annotation.Nullable;

// will be serialized by GSON
public class DimStackEntry {
    public String dimensionIdStr;
    public double scale = 1;
    public boolean flipped = false;
    public double horizontalRotation = 0;
    @Nullable
    public Integer topY = null;
    @Nullable
    public Integer bottomY = null;
    @Nullable
    public String bedrockReplacementStr = "minecraft:obsidian";
    public boolean connectsPrevious = true;
    public boolean connectsNext = true;
    
    public DimStackEntry(ResourceKey<Level> dimension) {
        this.dimensionIdStr = dimension.location().toString();
    }
    
    public DimStackEntry() {}
    
    public ResourceKey<Level> getDimension() {
        Validate.notNull(dimensionIdStr);
        return DimId.idToKey(dimensionIdStr);
    }
    
    public DimStackEntry copy() {
        DimStackEntry copy = new DimStackEntry();
        copy.dimensionIdStr = dimensionIdStr;
        copy.scale = scale;
        copy.flipped = flipped;
        copy.horizontalRotation = horizontalRotation;
        copy.topY = topY;
        copy.bottomY = bottomY;
        copy.bedrockReplacementStr = bedrockReplacementStr;
        copy.connectsPrevious = connectsPrevious;
        copy.connectsNext = connectsNext;
        return copy;
    }
}
