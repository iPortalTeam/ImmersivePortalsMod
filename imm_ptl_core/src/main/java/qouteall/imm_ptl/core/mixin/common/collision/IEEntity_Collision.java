package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(Entity.class)
public interface IEEntity_Collision {
    @Invoker("collideWithShapes")
    static Vec3 ip_CollideWithShapes(Vec3 vec3, AABB aABB, List<VoxelShape> list) {
        throw new UnsupportedOperationException();
    }
}
