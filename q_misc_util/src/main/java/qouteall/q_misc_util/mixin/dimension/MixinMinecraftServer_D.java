package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.api.DimensionAPI;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_D {
    @Shadow
    @Final
    protected SaveProperties saveProperties;
    
    @Shadow
    public abstract DynamicRegistryManager getRegistryManager();
    
    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void onBeforeCreateWorlds(
        WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        GeneratorOptions generatorOptions = saveProperties.getGeneratorOptions();
        
        DynamicRegistryManager registryManager = getRegistryManager();
    
        DimensionAPI.serverDimensionsLoadEvent.invoker().run(generatorOptions, registryManager);
        
    }
    
}
