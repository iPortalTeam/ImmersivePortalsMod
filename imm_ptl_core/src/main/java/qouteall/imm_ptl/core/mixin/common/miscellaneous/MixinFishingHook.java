package qouteall.imm_ptl.core.mixin.common.miscellaneous;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.api.ImmPtlEntityExtension;

@Mixin(FishingHook.class)
public class MixinFishingHook implements ImmPtlEntityExtension {
    @Override
    public boolean imm_ptl_canTeleportThroughPortal(Entity portal) {
        return false;
    }
}
