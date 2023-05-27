package qouteall.imm_ptl.core.mixin.common.other_sync;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public class MixinPlayerList_Test {
//    @WrapWithCondition(
//        method = "broadcast",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
//        )
//    )
//    public boolean vanish_hideGameEvents(
//        ServerGamePacketListenerImpl packetListener, Packet<?> packet, Player player
//    ) {
//        System.out.println("wow");
//        return true;
//    }
}
