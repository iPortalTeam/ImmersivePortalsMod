package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IEWorld;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public class MixinWorld implements IEWorld {
    
    @Shadow
    @Final
    protected MutableWorldProperties properties;
    
    @Override
    public MutableWorldProperties myGetProperties() {
        return properties;
    }
}
