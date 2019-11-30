package com.qouteall.immersive_portals.mixin_client;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.packet.EntityPassengersSetS2CPacket;
import net.minecraft.client.network.packet.PlayerPositionLookS2CPacket;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
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
    private boolean field_3698;
    
    @Shadow
    private MinecraftClient client;
    
    @Mutable
    @Shadow
    @Final
    private Map<UUID, PlayerListEntry> playerListEntries;
    
    @Shadow
    public abstract void onEntityPassengersSet(EntityPassengersSetS2CPacket entityPassengersSetS2CPacket_1);
    
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
    
    @Inject(
        method = "onPlayerPositionLook",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/ThreadExecutor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onProcessingPosistionPacket(
        PlayerPositionLookS2CPacket packet,
        CallbackInfo ci
    ) {
        DimensionType playerDimension = ((IEPlayerPositionLookS2CPacket) packet).getPlayerDimension();
        assert playerDimension != null;
        ClientWorld world = client.world;
        
        if (world != null) {
            if (world.dimension != null) {
                if (world.dimension.getType() != playerDimension) {
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
    
    }
    
    private boolean isReProcessingPassengerPacket;
    
    @Inject(
        method = "onEntityPassengersSet",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/ThreadExecutor;)V",
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
}
