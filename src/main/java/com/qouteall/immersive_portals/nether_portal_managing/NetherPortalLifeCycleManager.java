package com.qouteall.immersive_portals.nether_portal_managing;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;

public class NetherPortalLifeCycleManager {
    
    public static boolean checkInnerPortalBlocks(
        IWorld world,
        ObsidianFrame obsidianFrame
    ) {
        return obsidianFrame.boxWithoutObsidian.stream().allMatch(
            blockPos -> world.getBlockState(blockPos).getBlock()
                == BlockMyNetherPortal.instance
        );
    }
    
    public static void breakNetherPortal(
        NetherPortalGuard portalGuard
    ) {
        ServerWorld world1 = Helper.getServer().getWorld(portalGuard.dimension1);
        ServerWorld world2 = Helper.getServer().getWorld(portalGuard.dimension2);
        
        portalGuard.obsidianFrame1.boxWithoutObsidian.stream().forEach(
            blockPos -> world1.setBlockState(
                blockPos,
                Blocks.AIR.getDefaultState()
            )
        );
        
        portalGuard.obsidianFrame2.boxWithoutObsidian.stream().forEach(
            blockPos -> world2.setBlockState(
                blockPos,
                Blocks.AIR.getDefaultState()
            )
        );
        
        assert false;
    }
}
