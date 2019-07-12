package com.qouteall.immersive_portals.mixin;

import net.minecraft.block.PortalBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PortalBlock.class)
public class MixinPortalBlock {
    /**
     * @author qouteall
     */
    @Overwrite
    public boolean createPortalAt(IWorld iWorld_1, BlockPos blockPos_1) {
        return false;
    }
}
