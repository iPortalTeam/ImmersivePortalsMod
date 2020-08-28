package com.qouteall.immersive_portals.mixin_client.block_manipulation;

import com.qouteall.imm_ptl_peripheral.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.CGlobal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient_B {
    @Shadow
    protected abstract void doItemPick();
    
    @Shadow
    public ClientWorld world;
    
    @Shadow
    public HitResult crosshairTarget;
    
    @Shadow
    protected int attackCooldown;
    
    @Inject(
        method = "handleBlockBreaking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
        ),
        cancellable = true
    )
    private void onHandleBlockBreaking(boolean isKeyPressed, CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToPortal()) {
            BlockManipulationClient.myHandleBlockBreaking(isKeyPressed);
            ci.cancel();
        }
    }
    
    @Inject(
        method = "doAttack",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDoAttack(CallbackInfo ci) {
        if (attackCooldown <= 0) {
            if (BlockManipulationClient.isPointingToPortal()) {
                BlockManipulationClient.myAttackBlock();
                ci.cancel();
            }
        }
    }
    
    @Inject(
        method = "doItemUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"
        ),
        cancellable = true
    )
    private void onDoItemUse(CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToPortal()) {
            //TODO support offhand
            BlockManipulationClient.myItemUse(Hand.MAIN_HAND);
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "handleInputEvents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;doItemPick()V"
        )
    )
    private void redirectDoItemPick(MinecraftClient minecraftClient) {
        if (BlockManipulationClient.isPointingToPortal()) {
            ClientWorld remoteWorld = CGlobal.clientWorldLoader.getWorld(
                BlockManipulationClient.remotePointedDim
            );
            ClientWorld oldWorld = this.world;
            HitResult oldTarget = this.crosshairTarget;
            
            world = remoteWorld;
            crosshairTarget = BlockManipulationClient.remoteHitResult;
            
            doItemPick();
            
            world = oldWorld;
            crosshairTarget = oldTarget;
        }
        else {
            doItemPick();
        }
    }
}
