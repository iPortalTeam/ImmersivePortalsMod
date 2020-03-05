package com.qouteall.immersive_portals.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {
    @Shadow
    public ServerPlayerEntity player;
    
    @ModifyConstant(
        method = "processBlockBreakingAction",
        constant = @Constant(doubleValue = 36.0D)
    )
    private double modifyBreakBlockRangeSquare(double a) {
        double multiplier = HandReachTweak.getActualHandReachMultiplier(player);
        return a * multiplier * multiplier;
    }
}
