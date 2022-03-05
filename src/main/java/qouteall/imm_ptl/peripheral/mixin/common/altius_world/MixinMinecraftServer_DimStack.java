package qouteall.imm_ptl.peripheral.mixin.common.altius_world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.peripheral.altius_world.AltiusManagement;

import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_DimStack {
    @Shadow
    public abstract ServerLevel getLevel(ResourceKey<Level> dimensionType);
    
    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;setInitialSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/storage/ServerLevelData;ZZ)V"
        )
    )
    private void onBeforeSetupSpawn(ChunkProgressListener worldGenerationProgressListener, CallbackInfo ci) {
        AltiusManagement.onServerEarlyInit((MinecraftServer) (Object) this);
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V",
        at = @At("RETURN")
    )
    private void onCreateWorldsFinishes(
        ChunkProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        AltiusManagement.onServerCreatedWorlds((MinecraftServer) (Object) this);
    }
}
