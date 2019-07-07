package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEClientWorld;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    private ClientPlayNetworkHandler netHandler;
    
    @Override
    public ClientPlayNetworkHandler getNetHandler(){
        return netHandler;
    }
}
