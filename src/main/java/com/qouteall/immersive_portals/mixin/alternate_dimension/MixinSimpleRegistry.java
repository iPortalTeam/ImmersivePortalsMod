package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ducks.IESimpleRegistry;
import net.minecraft.util.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SimpleRegistry.class)
public class MixinSimpleRegistry implements IESimpleRegistry {

}
