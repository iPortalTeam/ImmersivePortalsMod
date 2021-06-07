package com.qouteall.hiding_in_the_bushes.mixin.alternate_dimension;

import com.qouteall.immersive_portals.network.CommonNetwork;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public class WorldMixin implements CommonNetwork.IMutableThread {

	@Shadow @Final private Thread thread;

	@Override
	public Thread getThread() {
		return this.thread;
	}
}

