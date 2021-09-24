package qouteall.imm_ptl.core.mixin.client.sync;

import com.mojang.authlib.GameProfile;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;
import qouteall.imm_ptl.core.dimension_sync.DimensionTypeSync;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.network.IPNetworkAdapt;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    
    @Shadow
    private DynamicRegistryManager registryManager;
    
    @Override
    public void ip_setWorld(ClientWorld world) {
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
        ClientWorldLoader.isFlatWorld = packet.isFlatWorld();
        DimensionTypeSync.onGameJoinPacketReceived(packet.getRegistryManager());
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
    private void onProcessingPositionPacket(
        PlayerPositionLookS2CPacket packet,
        CallbackInfo ci
    ) {
        if (!IPNetworkAdapt.doesServerHasIP()) {
            return;
        }
        
        if (!positionLookSetup) {
            // the first position packet removes the loading gui
            return;
        }
        
        RegistryKey<World> playerDimension = ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension();
        
        ClientWorld world = client.world;
        
        if (world != null) {
            if (world.getRegistryKey() != playerDimension) {
                if (!MinecraftClient.getInstance().player.isRemoved()) {
                    Helper.log(String.format(
                        "denied position packet %s %s %s %s",
                        ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension(),
                        packet.getX(), packet.getY(), packet.getZ()
                    ));
                    ci.cancel();
                }
            }
        }
        
        IPCGlobal.clientTeleportationManager.disableTeleportFor(5);
        
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
                IPGlobal.clientTaskList.addTask(() -> {
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
        if (IPCGlobal.smoothChunkUnload) {
            DimensionalChunkPos pos = new DimensionalChunkPos(
                world.getRegistryKey(), packet.getX(), packet.getZ()
            );
            
            WorldRenderer worldRenderer =
                ClientWorldLoader.getWorldRenderer(world.getRegistryKey());
            BuiltChunkStorage storage = ((IEWorldRenderer) worldRenderer).ip_getBuiltChunkStorage();
            if (storage instanceof MyBuiltChunkStorage) {
                ((MyBuiltChunkStorage) storage).onChunkUnload(packet.getX(), packet.getZ());
            }
            
            int[] counter = new int[1];
            counter[0] = (int) (Math.random() * 200);
            IPGlobal.clientTaskList.addTask(() -> {
                ClientWorld world1 = ClientWorldLoader.getWorld(pos.dimension);
                
                if (world1.getChunkManager().isChunkLoaded(pos.x, pos.z)) {
                    return true;
                }
                
                if (counter[0] > 0) {
                    counter[0]--;
                    return false;
                }
                
                WorldRenderer wr = ClientWorldLoader.getWorldRenderer(pos.dimension);
                
                Profiler profiler = MinecraftClient.getInstance().getProfiler();
                profiler.push("delayed_unload");
                
                for (int y = 0; y < 16; ++y) {
                    wr.scheduleBlockRenders(pos.x, y, pos.z);
                    world1.getLightingProvider().setSectionStatus(
                        ChunkSectionPos.from(pos.x, y, pos.z), true
                    );
                }
                
                world1.getLightingProvider().setColumnEnabled(pos.getChunkPos(), false);
                
                profiler.pop();
                
                return true;
            });
            ci.cancel();
        }
    }
    
    // for debug
    @Redirect(
        method = "onEntityTrackerUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;"
        )
    )
    private Entity redirectGetEntityById(ClientWorld clientWorld, int id) {
        Entity entity = clientWorld.getEntityById(id);
        if (entity == null) {
            Helper.err("missing entity for data tracking " + clientWorld + id);
        }
        return entity;
    }
    
    @Redirect(
        method = "onVelocityUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;setVelocityClient(DDD)V"
        )
    )
    private void redirectSetVelocityOnOnVelocityUpdate(Entity entity, double x, double y, double z) {
        if (!entity.isLogicalSideForUpdatingMovement()) {
            entity.setVelocityClient(x, y, z);
        }
        else {
            Helper.err("wrong velocity update packet " + entity);
        }
    }
    
    @Override
    public void portal_setRegistryManager(DynamicRegistryManager arg) {
        registryManager = arg;
    }
}
