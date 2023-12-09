package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEServerChunkManager;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCache implements IEServerChunkManager {
    @Shadow
    @Final
    private DistanceManager distanceManager;
    
    @Override
    public DistanceManager ip_getDistanceManager() {
        return distanceManager;
    }
}
