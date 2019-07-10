package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler implements IEClientPlayNetworkHandler {
    @Shadow
    private ClientWorld world;
    
    @Override
    public void setWorld(ClientWorld world) {
        this.world = world;
    }
    
}
