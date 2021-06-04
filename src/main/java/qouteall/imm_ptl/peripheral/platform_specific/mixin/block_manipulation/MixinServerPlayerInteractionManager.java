package qouteall.imm_ptl.peripheral.platform_specific.mixin.block_manipulation;

import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ServerPlayerInteractionManager.class, priority = 900)
public class MixinServerPlayerInteractionManager {

}
