package qouteall.imm_ptl.peripheral.platform_specific.mixin.block_manipulation;

import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ServerPlayerGameMode.class, priority = 900)
public class MixinServerPlayerInteractionManager {

}
