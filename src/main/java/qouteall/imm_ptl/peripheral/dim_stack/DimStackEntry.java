package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import qouteall.q_misc_util.dimension.DimId;

import javax.annotation.Nullable;

public class DimStackEntry {
    public ResourceKey<Level> dimension;
    public double scale = 1;
    public boolean flipped = false;
    public double horizontalRotation = 0;
    @Nullable
    public Integer topY = null;
    @Nullable
    public Integer bottomY = null;
    @Nullable
    public String bedrockReplacementStr = "minecraft:obsidian";
    
    public DimStackEntry(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }
    
    public CompoundTag toNbt() {
        CompoundTag compound = new CompoundTag();
        compound.putString("dimension", dimension.location().toString());
        compound.putDouble("scale", scale);
        compound.putBoolean("flipped", flipped);
        compound.putDouble("horizontalRotation", horizontalRotation);
        if (topY != null) {
            compound.putInt("topY", topY);
        }
        if (bottomY != null) {
            compound.putInt("bottomY", bottomY);
        }
        if (bedrockReplacementStr != null) {
            compound.putString("bedrockReplacement", bedrockReplacementStr);
        }
        return compound;
    }
    
    public static DimStackEntry fromNbt(CompoundTag compound) {
        ResourceKey<Level> dimension = DimId.idToKey(compound.getString("dimension"));
        DimStackEntry dimStackEntry = new DimStackEntry(dimension);
        dimStackEntry.scale = compound.contains("scale") ? compound.getDouble("scale") : 1.0;
        dimStackEntry.flipped = compound.contains("flipped") ? compound.getBoolean("flipped") : false;
        dimStackEntry.horizontalRotation = compound.getDouble("horizontalRotation");
        dimStackEntry.topY = compound.contains("topY") ? compound.getInt("topY") : null;
        dimStackEntry.bottomY = compound.contains("bottomY") ? compound.getInt("bottomY") : null;
        dimStackEntry.bedrockReplacementStr = compound.contains("bedrockReplacement") ?
            compound.getString("bedrockReplacement") : null;
        return dimStackEntry;
    }
}
