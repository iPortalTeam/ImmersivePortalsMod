package com.qouteall.hiding_in_the_bushes.mixin.client;

import com.qouteall.hiding_in_the_bushes.IEClientWorld_MA;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld_MA implements IEClientWorld_MA {
    
    @Shadow
    public abstract void removeEntity(int i);
    
    @Override
    public void segregateEntity(Entity entity) {
        removeEntity(entity.getEntityId());
    }
}
