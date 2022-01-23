package qouteall.imm_ptl.peripheral.platform_specific.mixin.block_manipulation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(value = Item.class, priority = 900)
public class MixinItem {
    private static WeakReference<Player> argPlayer;
    
    @Inject(
        method = "Lnet/minecraft/world/item/Item;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;",
        at = @At("HEAD")
    )
    private static void onRayTrace(
        Level world,
        Player player,
        ClipContext.Fluid fluidHandling,
        CallbackInfoReturnable<HitResult> cir
    ) {
        argPlayer = new WeakReference<>(player);
    }
    
}
