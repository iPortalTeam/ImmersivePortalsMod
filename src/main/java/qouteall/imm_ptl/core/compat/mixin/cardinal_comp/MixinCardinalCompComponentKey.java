package qouteall.imm_ptl.core.compat.mixin.cardinal_comp;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.component.sync.ComponentPacketWriter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.network.PacketRedirection;

@Mixin(ComponentKey.class)
public class MixinCardinalCompComponentKey {
    // redirect the entity sync packet
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
        method = "Ldev/onyxstudios/cca/api/v3/component/ComponentKey;syncWith(Lnet/minecraft/server/level/ServerPlayer;Ldev/onyxstudios/cca/api/v3/component/ComponentProvider;Ldev/onyxstudios/cca/api/v3/component/sync/ComponentPacketWriter;Ldev/onyxstudios/cca/api/v3/component/sync/PlayerSyncPredicate;)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/onyxstudios/cca/api/v3/component/ComponentProvider;toComponentPacket(Ldev/onyxstudios/cca/api/v3/component/ComponentKey;Ldev/onyxstudios/cca/api/v3/component/sync/ComponentPacketWriter;Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;"
        )
    )
    private ClientboundCustomPayloadPacket redirectPacket(
        ComponentProvider instance, ComponentKey<?> key, ComponentPacketWriter writer, ServerPlayer recipient
    ) {
        ClientboundCustomPayloadPacket packet =
            instance.toComponentPacket(key, writer, recipient);
        if (instance instanceof Entity entity) {
            var redirected = PacketRedirection.createRedirectedMessage(
                entity.getServer(),
                entity.level().dimension(),
                (Packet<ClientGamePacketListener>) (Packet) packet
            );
            packet = (ClientboundCustomPayloadPacket) (Packet) redirected;
        }
        return packet;
    }
}
