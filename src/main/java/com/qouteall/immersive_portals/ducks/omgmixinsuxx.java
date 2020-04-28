package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;

/**
 * I'm so disappointed in you Mixin
 *
 * @see com.qouteall.immersive_portals.mixin.MixinBlockView#rayTrace(RayTraceContext)
 */
public class omgmixinsuxx {
    public static BlockHitResult onRayTraceBlock(BlockView blockView, Vec3d start, Vec3d end, BlockPos pos, VoxelShape shape, BlockState state) {
        if (Global.portalPlaceholderPassthrough && state.getBlock() == PortalPlaceholderBlock.instance) {
            return null;
        }

        return blockView.rayTraceBlock(start, end, pos, shape, state);
    }
}
