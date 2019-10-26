package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.optifine_compatibility.ShaderCullingManager;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient implements IEMinecraftClient {
    @Shadow
    private GlFramebuffer framebuffer;
    
    @Shadow
    public Screen currentScreen;
    
    @Inject(at = @At("TAIL"), method = "init()V")
    private void onInitEnded(CallbackInfo info) {
        if (FabricLoader.INSTANCE.isModLoaded("optifabric")) {
            ShaderCullingManager.init();
        }
    }
    
    @Inject(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;tick(Ljava/util/function/BooleanSupplier;)V",
            shift = At.Shift.AFTER
        ),
        method = "Lnet/minecraft/client/MinecraftClient;tick()V"
    )
    private void onClientTick(CallbackInfo ci) {
        ModMain.postClientTickSignal.emit();
    }
    
    @Inject(
        method = "Lnet/minecraft/client/MinecraftClient;setWorld(Lnet/minecraft/client/world/ClientWorld;)V",
        at = @At("HEAD")
    )
    private void onSetWorld(ClientWorld clientWorld_1, CallbackInfo ci) {
        CGlobal.clientWorldLoader.cleanUp();
        if (CGlobal.isOptifinePresent) {
            //OFGlobal.shaderContextManager.cleanup();
        }
    }
    
    @Override
    public void setFrameBuffer(GlFramebuffer buffer) {
        framebuffer = buffer;
    }
    
    @Override
    public Screen getCurrentScreen() {
        return currentScreen;
    }
}
