package qouteall.q_misc_util.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import qouteall.q_misc_util.MiscHelper;

/**
 * A temporary workaround to make Polymer compat not break
 * https://github.com/Patbox/polymer/blob/dev/1.20.3/polymer-core/src/main/java/eu/pb4/polymer/core/impl/compat/ImmersivePortalsUtils.java
 */
@Deprecated
public class DimensionIdRecord {
    public static DimensionIdRecord serverRecord = new DimensionIdRecord();
    
    public ResourceKey<Level> getDim(int dimIntId) {
        return DimensionIntId.getServerMap(MiscHelper.getServer()).fromIntegerId(dimIntId);
    }
}
