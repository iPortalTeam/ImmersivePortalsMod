package com.qouteall.immersive_portals.mixin_client.sync;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler implements IEClientPlayNetworkHandler {
    @Shadow
    private ClientWorld world;
    
    @Shadow
    private boolean positionLookSetup;
    
    @Shadow
    private MinecraftClient client;
    
    @Mutable
    @Shadow
    @Final
    private Map<UUID, PlayerListEntry> playerListEntries;
    
    @Shadow
    public abstract void onEntityPassengersSet(EntityPassengersSetS2CPacket entityPassengersSetS2CPacket_1);
    
    @Shadow private RegistryTracker registryTracker;
    
    @Override
    public void setWorld(ClientWorld world) {
        this.world = world;
    }
    
    @Override
    public Map getPlayerListEntries() {
        return playerListEntries;
    }
    
    @Override
    public void setPlayerListEntries(Map value) {
        playerListEntries = value;
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onInit(
        MinecraftClient minecraftClient_1,
        Screen screen_1,
        ClientConnection clientConnection_1,
        GameProfile gameProfile_1,
        CallbackInfo ci
    ) {
        isReProcessingPassengerPacket = false;
    }
    
    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        DimensionTypeSync.onGameJoinPacketReceived(packet.getDimension());
    }
    
    @Inject(
        method = "onPlayerPositionLook",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onProcessingPosistionPacket(
        PlayerPositionLookS2CPacket packet,
        CallbackInfo ci
    ) {
        RegistryKey<World> playerDimension = ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension();
        assert playerDimension != null;
        ClientWorld world = client.world;
        
        if (client.player != null) {
            McHelper.checkDimension(client.player);
        }
        
        if (world != null) {
            if (world.getDimension() != null) {
                if (world.getRegistryKey() != playerDimension) {
                    if (!MinecraftClient.getInstance().player.removed) {
                        Helper.log(String.format(
                            "denied position packet %s %s %s %s",
                            ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension(),
                            packet.getX(), packet.getY(), packet.getZ()
                        ));
                        ci.cancel();
                    }
                }
            }
        }
        
        CGlobal.clientTeleportationManager.disableTeleportFor(20);
        
    }
    
    private boolean isReProcessingPassengerPacket;
    
    @Inject(
        method = "onEntityPassengersSet",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onOnEntityPassengersSet(
        EntityPassengersSetS2CPacket entityPassengersSetS2CPacket_1,
        CallbackInfo ci
    ) {
        Entity entity_1 = this.world.getEntityById(entityPassengersSetS2CPacket_1.getId());
        if (entity_1 == null) {
            if (!isReProcessingPassengerPacket) {
                Helper.log("Re-processed riding packet");
                ModMain.clientTaskList.addTask(() -> {
                    isReProcessingPassengerPacket = true;
                    onEntityPassengersSet(entityPassengersSetS2CPacket_1);
                    isReProcessingPassengerPacket = false;
                    return true;
                });
                ci.cancel();
            }
        }
    }
    
    //fix lag spike
    //this lag spike is more severe with many portals pointing to different area
    @Inject(
        method = "onUnloadChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientChunkManager;getLightingProvider()Lnet/minecraft/world/chunk/light/LightingProvider;"
        ),
        cancellable = true
    )
    private void onOnUnload(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        if (CGlobal.smoothChunkUnload) {
            DimensionalChunkPos pos = new DimensionalChunkPos(
                world.getRegistryKey(),
                packet.getX(),
                packet.getZ()
            );
            
            WorldRenderer worldRenderer =
                CGlobal.clientWorldLoader.getWorldRenderer(world.getRegistryKey());
            BuiltChunkStorage storage = ((IEWorldRenderer) worldRenderer).getBuiltChunkStorage();
            if (storage instanceof MyBuiltChunkStorage) {
                for (int y = 0; y < 16; ++y) {
                    ChunkBuilder.BuiltChunk builtChunk = ((MyBuiltChunkStorage) storage).provideBuiltChunk(
                        new BlockPos(
                            packet.getX() * 16,
                            y * 16,
                            packet.getZ() * 16
                        )
                    );
                    ((IEBuiltChunk) builtChunk).fullyReset();
                }
                
            }
            
            int[] counter = new int[1];
            counter[0] = (int) (Math.random() * 200);
            ModMain.clientTaskList.addTask(() -> {
                ClientWorld world1 = CGlobal.clientWorldLoader.getWorld(pos.dimension);
                
                if (world1.getChunkManager().isChunkLoaded(pos.x, pos.z)) {
                    return true;
                }
                
                if (counter[0] > 0) {
                    counter[0]--;
                    return false;
                }
                
                Profiler profiler = MinecraftClient.getInstance().getProfiler();
                profiler.push("delayed_unload");
                
                for (int y = 0; y < 16; ++y) {
                    world1.getLightingProvider().updateSectionStatus(
                        ChunkSectionPos.from(pos.x, y, pos.z), true
                    );
                }
                
                world1.getLightingProvider().setLightEnabled(pos.getChunkPos(), false);
                
                profiler.pop();
                
                return true;
            });
            ci.cancel();
        }
    }
    
    @Override
    public void initScreenIfNecessary() {
        if (!this.positionLookSetup) {
            this.positionLookSetup = true;
            this.client.openScreen((Screen) null);
        }
    }
    
    @Override
    public void portal_setDimensionTracker(RegistryTracker arg) {
        registryTracker = arg;
    }
}
