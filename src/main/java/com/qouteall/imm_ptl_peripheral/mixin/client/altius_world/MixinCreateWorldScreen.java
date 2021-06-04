package com.qouteall.imm_ptl_peripheral.mixin.client.altius_world;

import com.mojang.datafixers.util.Pair;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusManagement;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusScreen;
import com.qouteall.imm_ptl_peripheral.ducks.IECreateWorldScreen;
import qouteall.imm_ptl.core.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.File;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen extends Screen implements IECreateWorldScreen {
    @Shadow
    public abstract void removed();
    
    @Shadow
    protected DataPackSettings dataPackSettings;
    
    @Shadow @org.jetbrains.annotations.Nullable protected abstract Pair<File, ResourcePackManager> getScannedPack();
    
    private ButtonWidget altiusButton;
    
    @Nullable
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
    
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;init()V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        altiusButton = (ButtonWidget) this.addDrawableChild(new ButtonWidget(
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
            target = "Lnet/minecraft/client/MinecraftClient;createWorld(Ljava/lang/String;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;Lnet/minecraft/world/gen/GeneratorOptions;)V"
        )
    )
    private void redirectOnCreateLevel(
        MinecraftClient client, String worldName, LevelInfo levelInfo,
        DynamicRegistryManager.Impl registryTracker, GeneratorOptions generatorOptions
    ) {
        if (altiusScreen != null) {
            AltiusInfo info = altiusScreen.getAltiusInfo();
            
            if (info != null) {
                AltiusManagement.dimensionStackPortalsToGenerate = info;
                
                GameRules.BooleanRule rule = levelInfo.getGameRules().get(AltiusGameRule.dimensionStackKey);
                rule.set(true, null);
                
                Helper.log("Generating dimension stack world");
            }
        }
        
        client.createWorld(worldName, levelInfo, registryTracker, generatorOptions);
    }
    
    private void openAltiusScreen() {
        if (altiusScreen == null) {
            altiusScreen = new AltiusScreen((CreateWorldScreen) (Object) this);
        }
        
        MinecraftClient.getInstance().openScreen(altiusScreen);
    }
    
    @Override
    public ResourcePackManager portal_getResourcePackManager() {
        return getScannedPack().getSecond();
    }
    
    @Override
    public DataPackSettings portal_getDataPackSettings() {
        return dataPackSettings;
    }
}
