package qouteall.imm_ptl.core.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.mixin.common.other_sync.IEServerConfigurationPacketListenerImpl;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class ImmPtlNetworkConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static record ModVersion(
        int major, int minor, int patch
    ) {
        // for dev env
        public static final ModVersion OTHER = new ModVersion(0, 0, 0);
        
        public static ModVersion read(FriendlyByteBuf buf) {
            int major = buf.readVarInt();
            int minor = buf.readVarInt();
            int patch = buf.readVarInt();
            return new ModVersion(major, minor, patch);
        }
        
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(major);
            buf.writeVarInt(minor);
            buf.writeVarInt(patch);
        }
        
        @Override
        public String toString() {
            return "%d.%d.%d".formatted(major, minor, patch);
        }
        
        public boolean isNormalVersion() {
            return !OTHER.equals(this);
        }
        
        public boolean isCompatibleWith(ModVersion another) {
            return major == another.major && minor == another.minor;
        }
    }
    
    public static ModVersion immPtlVersion;
    
    @SuppressWarnings("UnstableApiUsage")
    public static record ImmPtlConfigurationTask(
    ) implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE =
            new ConfigurationTask.Type("imm_ptl_core:config");
        
        @Override
        public void start(Consumer<Packet<?>> consumer) {
            consumer.accept(
                ServerConfigurationNetworking.createS2CPacket(new S2CConfigStartPacket(
                    immPtlVersion
                ))
            );
        }
        
        @Override
        public @NotNull Type type() {
            return TYPE;
        }
    }
    
    public static record S2CConfigStartPacket(
        ModVersion versionFromServer
    ) implements FabricPacket {
        public static final PacketType<S2CConfigStartPacket> TYPE =
            PacketType.create(
                new ResourceLocation("imm_ptl_core:config_packet"),
                S2CConfigStartPacket::read
            );
        
        public static S2CConfigStartPacket read(FriendlyByteBuf buf) {
            ModVersion info = ModVersion.read(buf);
            return new S2CConfigStartPacket(info);
        }
        
        public void write(FriendlyByteBuf buf) {
            versionFromServer.write(buf);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        // handled on client side
        @Environment(EnvType.CLIENT)
        public void handle(PacketSender responseSender) {
            LOGGER.info(
                "Client received ImmPtl config packet. Server mod version: {}", versionFromServer
            );
            
            serverVersion = versionFromServer;
            responseSender.sendPacket(new C2SConfigCompletePacket(
                immPtlVersion, IPConfig.getConfig().clientTolerantVersionMismatchWithServer
            ));
            
            if (!versionFromServer.isNormalVersion() || !immPtlVersion.isNormalVersion()) {
                return;
            }
            
            if (!versionFromServer.equals(immPtlVersion)) {
                MyTaskList.MyTask task = MyTaskList.oneShotTask(() -> {
                    if (IPConfig.getConfig()
                        .shouldDisplayWarning("mod_version_mismatch")
                    ) {
                        MutableComponent text =
                            Component.translatable(
                                "imm_ptl.mod_patch_version_mismatch",
                                Component.literal(versionFromServer.toString())
                                    .withStyle(ChatFormatting.GOLD),
                                Component.literal(immPtlVersion.toString())
                                    .withStyle(ChatFormatting.GOLD)
                            ).append(
                                IPMcHelper.getDisableWarningText("mod_version_mismatch")
                            );
                        CHelper.printChat(text);
                    }
                });
                IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
                    () -> Minecraft.getInstance().level == null, task
                ));
            }
        }
    }
    
    public record C2SConfigCompletePacket(
        ModVersion versionFromClient,
        boolean clientTolerantVersionMismatch
    ) implements FabricPacket {
        public static final PacketType<C2SConfigCompletePacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl_core:configure_complete"),
            C2SConfigCompletePacket::read
        );
        
        public static C2SConfigCompletePacket read(FriendlyByteBuf buf) {
            ModVersion info = ModVersion.read(buf);
            boolean clientTolerantVersionMismatch = buf.readBoolean();
            return new C2SConfigCompletePacket(info, clientTolerantVersionMismatch);
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            versionFromClient.write(buf);
            buf.writeBoolean(clientTolerantVersionMismatch);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        // handled on server side
        public void handle(
            ServerConfigurationPacketListenerImpl networkHandler, PacketSender responseSender
        ) {
            GameProfile gameProfile =
                ((IEServerConfigurationPacketListenerImpl) networkHandler).ip_getGameProfile();
            
            LOGGER.info(
                "Server received ImmPtl config packet. Mod version: {} Player: {} {}",
                versionFromClient, gameProfile.getName(), gameProfile.getId()
            );
            
            if (versionFromClient.isNormalVersion() && immPtlVersion.isNormalVersion()) {
                if ((versionFromClient.major != immPtlVersion.major ||
                    versionFromClient.minor != immPtlVersion.minor) &&
                    !IPConfig.getConfig().serverTolerantVersionMismatchWithClient &&
                    !clientTolerantVersionMismatch
                ) {
                    networkHandler.disconnect(Component.translatable(
                        "imm_ptl.mod_major_minor_version_mismatch",
                        immPtlVersion.toString(),
                        versionFromClient.toString()
                    ));
                    LOGGER.info(
                        """
                            Disconnecting client because of ImmPtl version difference (only patch version difference is tolerated).
                            Game Profile: {}
                            Client ImmPtl version: {}
                            Server ImmPtl version: {}""",
                        gameProfile, versionFromClient, immPtlVersion
                    );
                    return;
                }
            }
            
            networkHandler.completeTask(ImmPtlConfigurationTask.TYPE);
        }
    }
    
    public static void init() {
        immPtlVersion = O_O.getImmPtlVersion();
        
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, S2CConfigStartPacket.TYPE)) {
                handler.addTask(new ImmPtlConfigurationTask());
            }
            else {
                // cannot use translation key here
                // because the translation does not exist on client without the mod
                handler.disconnect(Component.literal(
                    "The client should have Immersive Portals mod installed to join this server"
                ));
            }
        });
        
        ServerConfigurationNetworking.registerGlobalReceiver(
            C2SConfigCompletePacket.TYPE,
            C2SConfigCompletePacket::handle
        );
        
        LOGGER.info("Immersive Portals Core version {}", immPtlVersion);
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        // ClientConfigurationNetworking.ConfigurationPacketHandler does not provide
        // ClientConfigurationPacketListenerImpl argument
        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CConfigStartPacket.TYPE,
            S2CConfigStartPacket::handle
        );
        
        ClientLoginConnectionEvents.INIT.register(
            (handler, client) -> {
                LOGGER.info("Client login init");
                // if the config packet is not received,
                // serverProtocolInfo will always be nul
                // it will become not null when receiving ImmPtl config packet
                serverVersion = null;
            }
        );
        
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!doesServerHaveImmPtl()) {
                warnServerMissingImmPtl();
            }
        });
    }
    
    // used on client
    private static @Nullable ImmPtlNetworkConfig.ModVersion serverVersion = null;
    
    // should be called from client
    public static boolean doesServerHaveImmPtl() {
        return serverVersion != null;
    }
    
    private static void warnServerMissingImmPtl() {
        Minecraft.getInstance().execute(() -> {
            CHelper.printChat(Component.translatable("imm_ptl.server_missing_immptl"));
        });
    }
}
