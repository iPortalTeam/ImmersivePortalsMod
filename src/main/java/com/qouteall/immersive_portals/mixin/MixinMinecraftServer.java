package com.qouteall.immersive_portals.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.dimension_sync.DimensionIdManagement;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.MetricsData;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer implements IEMinecraftServer {
    @Shadow
    @Final
    private MetricsData metricsData;
    
    private boolean portal_areAllWorldsLoaded;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onServerConstruct(
        Thread thread, DynamicRegistryManager.Impl impl,
        LevelStorage.Session session, SaveProperties saveProperties,
        ResourcePackManager resourcePackManager, Proxy proxy, DataFixer dataFixer,
        ServerResourceManager serverResourceManager, MinecraftSessionService minecraftSessionService,
        GameProfileRepository gameProfileRepository, UserCache userCache,
        WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci
    ) {
        McHelper.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
    
        O_O.loadConfigFabric();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V",
        at = @At("TAIL")
    )
    private void onServerTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        ModMain.postServerTickSignal.emit();
    }
    
    @Inject(
        method = "runServer",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        NewChunkTrackingGraph.cleanup();
        ModMain.serverTaskList.forceClearTasks();
    }
    
    @Inject(
        method = "createWorlds",
        at = @At("RETURN")
    )
    private void onFinishedLoadingAllWorlds(
        CallbackInfo ci
    ) {
        portal_areAllWorldsLoaded = true;
        DimensionIdManagement.onServerStarted();
    }
    
    @Override
    public MetricsData getMetricsDataNonClientOnly() {
        return metricsData;
    }
    
    @Override
    public boolean portal_getAreAllWorldsLoaded() {
        return portal_areAllWorldsLoaded;
    }
}
