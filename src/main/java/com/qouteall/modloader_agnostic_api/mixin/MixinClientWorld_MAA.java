package com.qouteall.modloader_agnostic_api.mixin;

import com.qouteall.modloader_agnostic_api.IEClientWorldMAA;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld_MAA implements IEClientWorldMAA {
    
    @Shadow
    public abstract void removeEntity(int i);
    
    @Override
    public void removeEntityWhilstMaintainingCapability(Entity entity) {
        removeEntity(entity.getEntityId());
    }
}
