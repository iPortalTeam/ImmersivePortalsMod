package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.dimension_sync.DimId;

public class AltiusEntry {
    public RegistryKey<World> dimension;
    public double scale = 1;
    public boolean flipped = false;
    public double horizontalRotation = 0;
    
    public AltiusEntry(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }
    
    public NbtCompound toNbt() {
        NbtCompound compound = new NbtCompound();
        compound.putString("dimension", dimension.getValue().toString());
        compound.putDouble("scale", scale);
        compound.putBoolean("flipped", flipped);
        compound.putDouble("horizontalRotation", horizontalRotation);
        return compound;
    }
    
    public static AltiusEntry fromNbt(NbtCompound compound) {
        RegistryKey<World> dimension = DimId.idToKey(compound.getString("dimension"));
        AltiusEntry altiusEntry = new AltiusEntry(dimension);
        altiusEntry.scale = compound.contains("scale") ? compound.getDouble("scale") : 1.0;
        altiusEntry.flipped = compound.contains("flipped") ? compound.getBoolean("flipped") : false;
        altiusEntry.horizontalRotation = compound.getDouble("horizontalRotation");
        return altiusEntry;
    }
}
