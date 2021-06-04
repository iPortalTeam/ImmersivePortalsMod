package qouteall.imm_ptl.peripheral.platform_specific.mixin.block_manipulation;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 900)
public class MixinServerPlayNetworkHandler_B {

}
