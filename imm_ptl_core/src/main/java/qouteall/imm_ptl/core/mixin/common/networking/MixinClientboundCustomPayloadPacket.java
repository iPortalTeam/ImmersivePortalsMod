package qouteall.imm_ptl.core.mixin.common.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IECustomPayloadPacket;
import qouteall.imm_ptl.core.network.IPNetworking;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.dimension.DimId;

@Mixin(ClientboundCustomPayloadPacket.class)
public class MixinClientboundCustomPayloadPacket implements IECustomPayloadPacket {
    
    @Shadow
    @Final
    private ResourceLocation identifier;
    
    @Shadow
    @Final
    private FriendlyByteBuf data;
    
    // eliminate the size limitation
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
        constant = @Constant(intValue = 1048576),
        require = 0 // minecells also mixins this
        // https://github.com/mim1q/MineCells/blob/6f995b86e00bb1c8bbea86ca91e5cd7e54fce44c/src/main/java/com/github/mim1q/minecells/mixin/network/CustomPayloadS2CPacketMixin.java#L24
    )
    private int modifySizeLimitWhenReadingPacket(int oldValue) {
        return 233333333;
    }
    
    private ResourceKey<Level> ip_redirectedDimension;
    private Packet<ClientGamePacketListener> ip_redirectedPacket;
    
    @Inject(
        method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
        at = @At("RETURN")
    )
    private void readTheActualRedirectedPacket(
        FriendlyByteBuf _buf, CallbackInfo ci
    ) {
        if (PacketRedirection.isPacketIdOfRedirection(identifier)) {
            FriendlyByteBuf buf = data;
            
            ResourceKey<Level> dimension = DimId.readWorldId(buf, true);
            
            int packetId = buf.readInt();
            Packet packet = PacketRedirection.createPacketById(packetId, buf);
            if (packet == null) {
                throw new RuntimeException("Unknown packet id %d in %s".formatted(packetId, dimension.location()));
            }
            
            ip_redirectedDimension = dimension;
            ip_redirectedPacket = (Packet<ClientGamePacketListener>) packet;
        }
    }
    
    @Inject(
        method = "write",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/FriendlyByteBuf;writeBytes(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;"
        ),
        cancellable = true
    )
    private void writeTheActualRedirectedPacket(FriendlyByteBuf buffer, CallbackInfo ci) {
        if (PacketRedirection.isPacketIdOfRedirection(identifier)) {
            Validate.isTrue(ip_redirectedDimension != null, "ip_redirectedDimension is null");
            Validate.isTrue(ip_redirectedPacket != null, "ip_redirectedPacket is null");
            
            DimId.writeWorldId(buffer, ip_redirectedDimension, false);
            
            int packetId = PacketRedirection.getPacketId(ip_redirectedPacket);
            buffer.writeInt(packetId);
            
            ip_redirectedPacket.write(buffer);
            
            ci.cancel();
        }
    }
    
    // this is run before Fabric API try to handle the packet
    @Inject(
        method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHandle(ClientGamePacketListener handler, CallbackInfo ci) {
        if (PacketRedirection.isPacketIdOfRedirection(identifier)) {
            PacketRedirection.do_handleRedirectedPacket(
                ip_redirectedDimension, ip_redirectedPacket, handler
            );
            ci.cancel();
        }
        else {
            // NOTE should not change reader index in `data`
            boolean handled = IPNetworking.handleImmPtlCorePacketClientSide(
                identifier,
                
                // The slice packet has its own offset,
                // so that reading the data will not affect the original buffer.
                // It uses slice() instead of copy() to avoid copying the packet data.
                // In LAN multiplayer, the server owner will read the packet buffer,
                // but it will also need to write the packet concurrently for sending.
                
                // Note: in singleplayer and LAN server owner,
                // the normal packets are not serialized and deserialized. Just packet object passing.
                // But CustomPayloadPacket is special.
                // It involves its own buffer reading and writing,
                // even when the CustomPayloadPacket is not serialized.
                () -> new FriendlyByteBuf(data.slice())
            );
            if (handled) {
                ci.cancel();
            }
        }
    }
    
    // a redirected packet will be constructed with a dummy byte buf as argument
    // then its data is filled by the setters
    @Override
    public void ip_setRedirectedDimension(ResourceKey<Level> dimension) {
        ip_redirectedDimension = dimension;
    }
    
    @Override
    public void ip_setRedirectedPacket(Packet<ClientGamePacketListener> packet) {
        Validate.isTrue(!(packet instanceof BundlePacket<ClientGamePacketListener>));
        ip_redirectedPacket = packet;
    }
    
    @Override
    public ResourceKey<Level> ip_getRedirectedDimension() {
        return ip_redirectedDimension;
    }
    
    @Override
    public Packet<ClientGamePacketListener> ip_getRedirectedPacket() {
        return ip_redirectedPacket;
    }
}
