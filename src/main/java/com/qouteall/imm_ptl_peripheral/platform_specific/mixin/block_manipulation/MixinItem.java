package com.qouteall.imm_ptl_peripheral.platform_specific.mixin.block_manipulation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(value = Item.class, priority = 900)
public class MixinItem {
    private static WeakReference<PlayerEntity> argPlayer;
    
    @Inject(
        method = "raycast",
        at = @At("HEAD")
    )
    private static void onRayTrace(
        World world,
        PlayerEntity player,
        RaycastContext.FluidHandling fluidHandling,
        CallbackInfoReturnable<HitResult> cir
    ) {
        argPlayer = new WeakReference<>(player);
    }
    
}
