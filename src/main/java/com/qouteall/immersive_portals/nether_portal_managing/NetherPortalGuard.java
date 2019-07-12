package com.qouteall.immersive_portals.nether_portal_managing;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_utils.Helper;
import com.qouteall.immersive_portals.my_utils.auto_serialization.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;

@AutoSerializable
public class NetherPortalGuard {
    public final DimensionType dimension1;
    public final ObsidianFrame obsidianFrame1;
    public final DimensionType dimension2;
    public final ObsidianFrame obsidianFrame2;
    public final int primaryPortalId;
    
    public NetherPortalGuard(
        DimensionType dimension1,
        ObsidianFrame obsidianFrame1,
        DimensionType dimension2,
        ObsidianFrame obsidianFrame2,
        int primaryPortalId
    ) {
        this.dimension1 = dimension1;
        this.obsidianFrame1 = obsidianFrame1;
        this.dimension2 = dimension2;
        this.obsidianFrame2 = obsidianFrame2;
        this.primaryPortalId = primaryPortalId;
    }
    
    //if the region is not loaded, it will return true
    public boolean checkNetherPortalIfLoaded() {
        assert Helper.getServer() != null;
        
        return checkObsidianFrameIfLoaded(dimension1, obsidianFrame1) &&
            checkObsidianFrameIfLoaded(dimension2, obsidianFrame2);
    }
    
    //if the region is not loaded, it will return true
    private boolean checkObsidianFrameIfLoaded(
        DimensionType dimension,
        ObsidianFrame obsidianFrame
    ) {
        ServerWorld world = DimensionManager.getWorld(
            Helper.getServer(),
            dimension,
            false, false
        );
        
        if (world == null) {
            return true;
        }
        
        if (!world.isBlockLoaded(obsidianFrame.boxWithoutObsidian.l)) {
            return true;
        }
        
        if (!NetherPortalMatcher.checkObsidianFrame(
            world,
            obsidianFrame.normalAxis,
            obsidianFrame.boxWithoutObsidian
        )) {
            return false;
        }
        
        return NetherPortalLifeCycleManager.checkInnerPortalBlocks(world, obsidianFrame);
    }
}
