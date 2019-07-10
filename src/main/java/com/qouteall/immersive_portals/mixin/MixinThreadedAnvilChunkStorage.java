package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThreadedAnvilChunkStorage.class)
public class MixinThreadedAnvilChunkStorage implements IEThreadedAnvilChunkStorage {
    @Shadow
    int watchDistance;
    
    @Override
    public int getWatchDistance() {
        return watchDistance;
    }
}
