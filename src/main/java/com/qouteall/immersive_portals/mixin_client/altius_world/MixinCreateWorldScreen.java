package com.qouteall.immersive_portals.mixin_client.altius_world;

import com.google.gson.JsonElement;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.altius_world.AltiusScreen;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public class MixinCreateWorldScreen extends Screen {
    private ButtonWidget altiusButton;
    private AltiusScreen altiusScreen;
    
    protected MixinCreateWorldScreen(Text title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstructEnded(Screen parent, CallbackInfo ci) {
        altiusScreen = new AltiusScreen((Screen) (Object) this);
    }
    
    @Inject(
        method = "init",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        altiusButton = (ButtonWidget) this.addButton(new ButtonWidget(
            this.width / 2 - 75, 187 - 25, 150, 20,
            I18n.translate("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openAltiusScreen();
            }
        ));
        altiusButton.visible = true;
    
    }
    
    @Inject(
        method = "setMoreOptionsOpen",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            altiusButton.visible = false;
        }
        else {
            altiusButton.visible = true;
        }
    }
    
    @Redirect(
        method = "createLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelInfo;setGeneratorOptions(Lcom/google/gson/JsonElement;)Lnet/minecraft/world/level/LevelInfo;"
        )
    )
    private LevelInfo redirectOnCreateLevel(
        LevelInfo levelInfo, JsonElement generatorOptions
    ) {
        AltiusInfo info = altiusScreen.getAltiusInfo();
        ((IELevelProperties) (Object) levelInfo).setAltiusInfo(info);
    
        return levelInfo.setGeneratorOptions(generatorOptions);
    }
    
    private void openAltiusScreen() {
        MinecraftClient.getInstance().openScreen(altiusScreen);
    }
}
