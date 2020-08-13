package com.qouteall.immersive_portals.mixin_client.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.altius_world.AltiusScreen;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen extends Screen {
    @Shadow
    public abstract void removed();
    
    private ButtonWidget altiusButton;
    private AltiusScreen altiusScreen;
    
    protected MixinCreateWorldScreen(Text title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/resource/DataPackSettings;Lnet/minecraft/client/gui/screen/world/MoreOptionsDialog;)V",
        at = @At("RETURN")
    )
    private void onConstructEnded(
        Screen screen, DataPackSettings dataPackSettings, MoreOptionsDialog moreOptionsDialog,
        CallbackInfo ci
    ) {
        altiusScreen = new AltiusScreen((CreateWorldScreen) (Object) this);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;init()V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        altiusButton = (ButtonWidget) this.addButton(new ButtonWidget(
            width / 2 + 5, 151, 150, 20,
            new TranslatableText("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openAltiusScreen();
            }
        ));
        altiusButton.visible = false;
        
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;setMoreOptionsOpen(Z)V",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            altiusButton.visible = true;
        }
        else {
            altiusButton.visible = false;
        }
    }
    
    @Redirect(
        method = "createLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;method_29607(Ljava/lang/String;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;Lnet/minecraft/world/gen/GeneratorOptions;)V"
        )
    )
    private void redirectOnCreateLevel(
        MinecraftClient client, String worldName, LevelInfo levelInfo,
        DynamicRegistryManager.Impl registryTracker, GeneratorOptions generatorOptions
    ) {
        AltiusInfo info = altiusScreen.getAltiusInfo();
        ((IELevelProperties) (Object) levelInfo).setAltiusInfo(info);
    
        client.method_29607(worldName, levelInfo, registryTracker, generatorOptions);
    }
    
    private void openAltiusScreen() {
        MinecraftClient.getInstance().openScreen(altiusScreen);
    }
}
