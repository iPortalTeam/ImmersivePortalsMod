package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEMinecraftServer;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IEMinecraftServer {
    @Shadow
    @Final
    private FrameTimer frameTimer;
    
    @Shadow
    public abstract ProfilerFiller getProfiler();
    
    @Shadow
    @Final
    public LevelStorageSource.LevelStorageAccess storageSource;
    
    private boolean portal_areAllWorldsLoaded;
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V",
        at = @At("TAIL")
    )
    private void onServerTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        getProfiler().push("imm_ptl_tick");
        IPGlobal.postServerTickSignal.emit();
        getProfiler().pop();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;runServer()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        IPGlobal.serverCleanupSignal.emit();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V",
        at = @At("RETURN")
    )
    private void onFinishedLoadingAllWorlds(
        CallbackInfo ci
    ) {
        portal_areAllWorldsLoaded = true;
    }
    
    @Override
    public FrameTimer getMetricsDataNonClientOnly() {
        return frameTimer;
    }
    
    @Override
    public boolean portal_getAreAllWorldsLoaded() {
        return portal_areAllWorldsLoaded;
    }
}
