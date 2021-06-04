package qouteall.imm_ptl.core.portal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Mirror extends Portal {
    public static EntityType<Mirror> entityType;
    
    public Mirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public void tick() {
        super.tick();
        teleportable = false;
        setInteractable(false);
    }
    
    @Override
    public Vec3d transformLocalVecNonScale(Vec3d localVec) {
        return getMirrored(super.transformLocalVecNonScale(localVec));
    }
    
    @Override
    public boolean canTeleportEntity(Entity entity) {
        return false;
    }
    
    public Vec3d getMirrored(Vec3d vec) {
        double len = vec.dotProduct(getNormal());
        return vec.add(getNormal().multiply(len * -2));
    }
    
    @Override
    public Vec3d inverseTransformLocalVecNonScale(Vec3d localVec) {
        return super.inverseTransformLocalVecNonScale(getMirrored(localVec));
    }
}
