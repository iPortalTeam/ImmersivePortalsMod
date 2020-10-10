package com.qouteall.hiding_in_the_bushes.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 900)
public class MixinServerPlayNetworkHandler_B {
    @Shadow
    public ServerPlayerEntity player;
    
    @ModifyConstant(
        method = "onPlayerInteractBlock",
        constant = @Constant(doubleValue = 64.0D),
        require = 0
    )
    private double modifyPlacingBlockRangeSquared(double original) {
        double multiplier = HandReachTweak.getActualHandReachMultiplier(player);
        return original * multiplier * multiplier;
    }
}
