package qouteall.imm_ptl.core.portal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import qouteall.q_misc_util.my_util.DQuaternion;

public class Mirror extends Portal {
    public static final EntityType<Mirror> ENTITY_TYPE = Portal.createPortalEntityType(Mirror::new);
    
    public Mirror(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }
    
    @Override
    public void tick() {
        super.tick();
        setTeleportable(false);
        setInteractable(false);
    }
    
    // rotate before mirror
    @Override
    public Vec3 transformLocalVecNonScale(Vec3 localVec) {
        return getMirrored(super.transformLocalVecNonScale(localVec));
    }
    
    @Override
    public boolean canTeleportEntity(Entity entity) {
        return false;
    }
    
    public Vec3 getMirrored(Vec3 vec) {
        Vec3 normal = getNormal();
        return mirroredVec(vec, normal);
    }
    
    public static Vec3 mirroredVec(Vec3 vec, Vec3 normal) {
        double len = vec.dot(normal);
        return vec.add(normal.scale(len * -2));
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
    
    /**
     * the mirror's transformation: firstly rotate, then mirror
     * totalTrans = mirror * rotation
     * the mirror transform is applied after rotation, so the reflection direction is rotated, which is not what we want
     * to make the new mirror's rotation to visually match, we need
     * visualRotation = newRotation * mirror
     * newRotation = visualRotation * mirror^-1
     * mirror = mirror^-1
     */
    public void setRotationTransformationForMirror(DQuaternion visualRotation) {
        Matrix3d mirrorTrans = new Matrix3d().reflect(
            getNormal().x, getNormal().y, getNormal().z
        );
        Matrix3d visualRotationTrans = new Matrix3d().rotate(visualRotation.toMcQuaternion());
        Matrix3d newRotation = new Matrix3d().mul(visualRotationTrans).mul(mirrorTrans);
        Quaterniond mirrorRotation = new Quaterniond().setFromNormalized(newRotation);
        setRotation(DQuaternion.fromMcQuaternion(mirrorRotation));
    }
}
