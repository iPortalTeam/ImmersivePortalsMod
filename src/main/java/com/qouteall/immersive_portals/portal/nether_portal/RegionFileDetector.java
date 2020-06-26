package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.RegionBasedStorage;

import java.io.File;

public class RegionFileDetector {
    
    /**
     * {@link RegionBasedStorage#getRegionFile(ChunkPos)}
     */
    public static boolean doesRegionFileExist(
        ServerWorld world,
        ChunkPos pos
    ) {
        File saveDir = McHelper.getIEStorage(world.getRegistryKey()).portal_getSaveDir();
    
        File regionDir = new File(saveDir, "region");
        
        File file = new File(regionDir, "r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
    
        return file.exists();
    }
}
