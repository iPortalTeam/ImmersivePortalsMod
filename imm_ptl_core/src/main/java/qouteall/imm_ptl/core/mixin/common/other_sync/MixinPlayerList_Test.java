package qouteall.imm_ptl.core.mixin.common.other_sync;

import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;

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
