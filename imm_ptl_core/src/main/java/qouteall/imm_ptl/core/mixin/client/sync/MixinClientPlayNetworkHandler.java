package qouteall.imm_ptl.core.mixin.client.sync;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.telemetry.TelemetrySender;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
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
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.dimension_sync.DimensionTypeSync;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.network.IPNetworkAdapt;
import qouteall.q_misc_util.Helper;

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
    
    @Shadow
    protected abstract void readLightData(int x, int z, LightData data);
    
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
        MinecraftClient client,
        Screen screen, ClientConnection connection,
        GameProfile profile, TelemetrySender telemetrySender, CallbackInfo ci
    ) {
        isReProcessingPassengerPacket = false;
    }
    
    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        ClientWorldLoader.isFlatWorld = packet.flatWorld();
        DimensionTypeSync.onGameJoinPacketReceived(packet.registryManager());
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
        method = "onEntityVelocityUpdate",
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
