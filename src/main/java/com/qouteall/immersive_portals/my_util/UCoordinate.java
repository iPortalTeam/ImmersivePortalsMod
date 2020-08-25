package com.qouteall.immersive_portals.my_util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public class UCoordinate {
    public RegistryKey<World> dimension;
    public Vec3d pos;
    
    public UCoordinate(RegistryKey<World> dimension, Vec3d pos) {
        Validate.notNull(dimension);
        Validate.notNull(pos);
        this.dimension = dimension;
        this.pos = pos;
    }
    
    public UCoordinate(Entity entity) {
        this(entity.world.getRegistryKey(), entity.getPos());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UCoordinate that = (UCoordinate) o;
        return dimension.equals(that.dimension) &&
            pos.equals(that.pos);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s %s", dimension.getValue(), pos.x, pos.y, pos.z);
    }
}
