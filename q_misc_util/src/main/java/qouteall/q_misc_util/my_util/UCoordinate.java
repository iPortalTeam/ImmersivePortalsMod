package qouteall.q_misc_util.my_util;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// TODO remove in 1.20
@Deprecated
public class UCoordinate {
    public ResourceKey<Level> dimension;
    public Vec3 pos;
    
    public UCoordinate(ResourceKey<Level> dimension, Vec3 pos) {
        Validate.notNull(dimension);
        Validate.notNull(pos);
        this.dimension = dimension;
        this.pos = pos;
    }
    
    public UCoordinate(Entity entity) {
        this(entity.level.dimension(), entity.position());
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
        return String.format("%s %s %s %s", dimension.location(), pos.x, pos.y, pos.z);
    }
}
