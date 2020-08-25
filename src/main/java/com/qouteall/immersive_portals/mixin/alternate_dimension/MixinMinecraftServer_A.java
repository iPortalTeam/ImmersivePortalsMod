package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.alternate_dimension.AlternateDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.dimension.DimensionOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_A {
    @Shadow
    @Final
    protected SaveProperties saveProperties;
    
    @Shadow
    public abstract DynamicRegistryManager getRegistryManager();
    
    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void onBeforeCreateWorlds(
        WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        SimpleRegistry<DimensionOptions> registry = saveProperties.getGeneratorOptions().getDimensions();
        
        DynamicRegistryManager rm = getRegistryManager();
        
        long seed = saveProperties.getGeneratorOptions().getSeed();
        
        if (Global.enableAlternateDimensions) {
            AlternateDimensions.addAlternateDimensions(registry, rm, seed);
        }
    }
    
}
