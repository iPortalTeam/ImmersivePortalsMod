package qouteall.imm_ptl.core.portal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
}
