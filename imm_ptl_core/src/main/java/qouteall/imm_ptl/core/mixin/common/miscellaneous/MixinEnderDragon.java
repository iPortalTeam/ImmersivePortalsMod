package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.api.ImmPtlEntityExtension;

@Mixin(EnderDragon.class)
public class MixinEnderDragon implements ImmPtlEntityExtension {
    @Override
    public boolean imm_ptl_canTeleportThroughPortal(Entity portal) {
        return false;
    }
}
