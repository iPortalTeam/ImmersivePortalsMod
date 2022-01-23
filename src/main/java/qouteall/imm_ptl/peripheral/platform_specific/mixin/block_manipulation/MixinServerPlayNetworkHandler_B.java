package qouteall.imm_ptl.peripheral.platform_specific.mixin.block_manipulation;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 900)
public class MixinServerPlayNetworkHandler_B {

}
