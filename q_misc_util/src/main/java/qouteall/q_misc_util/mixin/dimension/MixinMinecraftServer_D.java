package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.WorldData;
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
    protected WorldData worldData;
    
    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();
    
    @Inject(method = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V", at = @At("HEAD"))
    private void onBeforeCreateWorlds(
        ChunkProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        WorldGenSettings generatorOptions = worldData.worldGenSettings();
        
        RegistryAccess registryManager = registryAccess();
        
        DimensionAPI.serverDimensionsLoadEvent.invoker().run(generatorOptions, registryManager);
        
    }
}
