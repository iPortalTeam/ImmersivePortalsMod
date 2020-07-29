package com.qouteall.hiding_in_the_bushes.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(value = Item.class, priority = 900)
public class MixinItem {
    private static WeakReference<PlayerEntity> argPlayer;
    
    @Inject(
        method = "rayTrace",
        at = @At("HEAD")
    )
    private static void onRayTrace(
        World world,
        PlayerEntity player,
        RayTraceContext.FluidHandling fluidHandling,
        CallbackInfoReturnable<HitResult> cir
    ) {
        argPlayer = new WeakReference<>(player);
    }
    
    @ModifyConstant(
        method = "rayTrace",
        constant = @Constant(doubleValue = 5.0D),
        require = 0
    )
    private static double modifyHandReach(double original) {
        double multiplier = HandReachTweak.getActualHandReachMultiplier(argPlayer.get());
        return original * multiplier;
    }
    
}
