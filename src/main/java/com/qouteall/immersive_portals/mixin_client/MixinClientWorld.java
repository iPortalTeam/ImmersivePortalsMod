package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEWorld;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    @Mutable
    private ClientPlayNetworkHandler netHandler;
    
    private List<GlobalTrackedPortal> globalTrackedPortals;
    
    @Override
    public ClientPlayNetworkHandler getNetHandler() {
        return netHandler;
    }
    
    @Override
    public void setNetHandler(ClientPlayNetworkHandler handler) {
        netHandler = handler;
    }
    
    @Override
    public List<GlobalTrackedPortal> getGlobalPortals() {
        return globalTrackedPortals;
    }
    
    @Override
    public void setGlobalPortals(List<GlobalTrackedPortal> arg) {
        globalTrackedPortals = arg;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;<init>(Lnet/minecraft/client/network/ClientPlayNetworkHandler;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/world/dimension/DimensionType;ILnet/minecraft/util/profiler/Profiler;Lnet/minecraft/client/render/WorldRenderer;)V",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPlayNetworkHandler clientPlayNetworkHandler_1,
        LevelInfo levelInfo_1,
        DimensionType dimensionType_1,
        int int_1,
        Profiler profiler_1,
        WorldRenderer worldRenderer_1,
        CallbackInfo ci
    ) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        MyClientChunkManager chunkManager = new MyClientChunkManager(clientWorld, int_1);
        ((IEWorld) this).setChunkManager(chunkManager);
    }
    
    //avoid entity duplicate when an entity travels
    @Inject(
        method = "addEntityPrivate",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        CGlobal.clientWorldLoader.clientWorldMap.values().stream()
            .filter(world -> world != (Object) this)
            .forEach(world -> world.removeEntity(entityId));
    }
    
    //avoid alternate dimension dark
    @Inject(
        method = "getSkyDarknessHeight",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onGetSkyDarknessHeight(CallbackInfoReturnable<Double> cir) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        if (clientWorld.dimension.getType() == ModMain.alternate1) {
            cir.setReturnValue(-100d);
            cir.cancel();
        }
    }
}
