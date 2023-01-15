package qouteall.imm_ptl.core.portal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import qouteall.q_misc_util.my_util.DQuaternion;

public class Mirror extends Portal {
    public static EntityType<Mirror> entityType;
    
    public Mirror(EntityType<?> entityType_1, Level world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public void tick() {
        super.tick();
        teleportable = false;
        setInteractable(false);
    }
    
    @Override
    public Vec3 transformLocalVecNonScale(Vec3 localVec) {
        return getMirrored(super.transformLocalVecNonScale(localVec));
    }
    
    @Override
    public boolean canTeleportEntity(Entity entity) {
        return false;
    }
    
    public Vec3 getMirrored(Vec3 vec) {
        double len = vec.dot(getNormal());
        return vec.add(getNormal().scale(len * -2));
    }
    
    @Override
    public Vec3 inverseTransformLocalVecNonScale(Vec3 localVec) {
        return super.inverseTransformLocalVecNonScale(getMirrored(localVec));
    }
    
    @Override
    public Matrix4d getFullSpaceTransformation() {
        Vec3 originPos = getOriginPos();
        Vec3 destPos = getDestPos();
        DQuaternion rot = getRotationD();
        return new Matrix4d()
            .translation(destPos.x, destPos.y, destPos.z)
            .reflect(getNormal().x, getNormal().y, getNormal().z, 0)
            .scale(getScale())
            .rotate(rot.toMcQuaternion())
            .translate(-originPos.x, -originPos.y, -originPos.z);
    }
}
