package com.qouteall.immersive_portals.mixin_client.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
            I18n.translate("imm_ptl.altius_screen"),
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
    
    private void openAltiusScreen() {
        MinecraftClient.getInstance().openScreen(altiusScreen);
    }
}
