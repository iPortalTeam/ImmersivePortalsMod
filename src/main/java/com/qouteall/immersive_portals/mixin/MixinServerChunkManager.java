package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IEServerChunkManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager implements IEServerChunkManager {

}
