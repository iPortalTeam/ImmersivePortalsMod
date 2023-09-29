package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(Player.class)
public abstract class MixinPlayer_Collision {
    @Shadow public abstract EntityDimensions getDimensions(Pose pose);
    
    /**
     * @author qouteall
     * @reason mixin does not allow cancel in redirect
     */
    @Overwrite
    public boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
        Entity this_ = (Entity) (Object) this;
        
        AABB box = getDimensions(pose).makeBoundingBox(this_.position());
        AABB activeCollisionBox = ((IEEntity) this_).ip_getActiveCollisionBox(box);
        if (activeCollisionBox == null) {
            return true;
        }
        return this_.level().noCollision(
            this_, activeCollisionBox.deflate(1.0E-7)
            // TODO check the issue of wrongly crouch after going through a scaling portal
            //  when head is touching ceiling because of floating point error
        );
    }
}
