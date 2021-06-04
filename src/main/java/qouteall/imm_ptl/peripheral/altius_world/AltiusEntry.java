package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class AltiusEntry {
    public RegistryKey<World> dimension;
    public double scale = 1;
    public boolean flipped = false;
    public double horizontalRotation = 0;
    
    public AltiusEntry(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }
}
