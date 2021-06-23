package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.immersive_portals.ducks.IMutableThread;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public class WorldMixin implements IMutableThread {

	@Shadow @Final private Thread thread;

	@Override
	public Thread getThread() {
		return this.thread;
	}
}

