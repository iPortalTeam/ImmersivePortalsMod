package qouteall.imm_ptl.core.mixin.client.block_manipulation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft_B {
    @Shadow
    protected abstract void pickBlock();
    
    @Shadow
    public ClientLevel level;
    
    @Shadow
    public HitResult hitResult;
    
    @Shadow
    protected int missTime;
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
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
        method = "Lnet/minecraft/client/Minecraft;startAttack()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDoAttack(CallbackInfo ci) {
        if (missTime <= 0) {
            if (BlockManipulationClient.isPointingToPortal()) {
                BlockManipulationClient.myAttackBlock();
                ci.cancel();
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;startUseItem()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
        ),
        cancellable = true
    )
    private void onDoItemUse(CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToPortal()) {
            // supporting offhand is unnecessary
            BlockManipulationClient.myItemUse(InteractionHand.MAIN_HAND);
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/Minecraft;handleKeybinds()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;pickBlock()V"
        )
    )
    private void redirectDoItemPick(Minecraft minecraftClient) {
        if (BlockManipulationClient.isPointingToPortal()) {
            ClientLevel remoteWorld = ClientWorldLoader.getWorld(BlockManipulationClient.remotePointedDim);
            ClientLevel oldWorld = this.level;
            HitResult oldTarget = this.hitResult;
            
            level = remoteWorld;
            hitResult = BlockManipulationClient.remoteHitResult;
            
            pickBlock();
            
            level = oldWorld;
            hitResult = oldTarget;
        }
        else {
            pickBlock();
        }
    }
}
