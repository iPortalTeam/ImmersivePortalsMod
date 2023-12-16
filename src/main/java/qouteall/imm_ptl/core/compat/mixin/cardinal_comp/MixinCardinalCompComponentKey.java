package qouteall.imm_ptl.core.compat.mixin.cardinal_comp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.network.PacketRedirection;

@Pseudo
@Mixin(targets = "dev.onyxstudios.cca.api.v3.component.ComponentKey")
public class MixinCardinalCompComponentKey {
    // redirect the entity sync packet
    @SuppressWarnings({"unchecked", "rawtypes"})
    @WrapOperation(
        method = "Ldev/onyxstudios/cca/api/v3/component/ComponentKey;syncWith(Lnet/minecraft/server/level/ServerPlayer;Ldev/onyxstudios/cca/api/v3/component/ComponentProvider;Ldev/onyxstudios/cca/api/v3/component/sync/ComponentPacketWriter;Ldev/onyxstudios/cca/api/v3/component/sync/PlayerSyncPredicate;)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/onyxstudios/cca/api/v3/component/ComponentProvider;toComponentPacket(Ldev/onyxstudios/cca/api/v3/component/ComponentKey;Ldev/onyxstudios/cca/api/v3/component/sync/ComponentPacketWriter;Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;"
        )
    )
    private ClientboundCustomPayloadPacket redirectPacket(
        @Coerce Object instance, @Coerce Object key,
        @Coerce Object writer, ServerPlayer recipient,
        Operation<Packet<?>> operation
    ) {
        Packet<?> packet = operation.call(instance, key, writer, recipient);
        
        if (instance instanceof Entity entity) {
            var redirected = PacketRedirection.createRedirectedMessage(
                entity.getServer(),
                entity.level().dimension(),
                (Packet<ClientGamePacketListener>) (Packet) packet
            );
            packet = redirected;
        }
        else if (instance instanceof BlockEntity blockEntity) {
            var redirected = PacketRedirection.createRedirectedMessage(
                blockEntity.getLevel().getServer(),
                blockEntity.getLevel().dimension(),
                (Packet<ClientGamePacketListener>) (Packet) packet
            );
            packet = redirected;
        }
        
        return (ClientboundCustomPayloadPacket) packet;
    }
}
