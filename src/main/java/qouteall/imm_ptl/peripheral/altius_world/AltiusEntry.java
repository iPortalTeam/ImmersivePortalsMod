package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.dimension_sync.DimId;

import javax.annotation.Nullable;

public class AltiusEntry {
    public RegistryKey<World> dimension;
    public double scale = 1;
    public boolean flipped = false;
    public double horizontalRotation = 0;
    @Nullable
    public Integer topY = null;
    @Nullable
    public Integer bottomY = null;
    @Nullable
    public String bedrockReplacementStr = "minecraft:obsidian";
    
    public AltiusEntry(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }
    
    public NbtCompound toNbt() {
        NbtCompound compound = new NbtCompound();
        compound.putString("dimension", dimension.getValue().toString());
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
    
    public static AltiusEntry fromNbt(NbtCompound compound) {
        RegistryKey<World> dimension = DimId.idToKey(compound.getString("dimension"));
        AltiusEntry altiusEntry = new AltiusEntry(dimension);
        altiusEntry.scale = compound.contains("scale") ? compound.getDouble("scale") : 1.0;
        altiusEntry.flipped = compound.contains("flipped") ? compound.getBoolean("flipped") : false;
        altiusEntry.horizontalRotation = compound.getDouble("horizontalRotation");
        altiusEntry.topY = compound.contains("topY") ? compound.getInt("topY") : null;
        altiusEntry.bottomY = compound.contains("bottomY") ? compound.getInt("bottomY") : null;
        altiusEntry.bedrockReplacementStr = compound.contains("bedrockReplacement") ?
            compound.getString("bedrockReplacement") : null;
        return altiusEntry;
    }
}
