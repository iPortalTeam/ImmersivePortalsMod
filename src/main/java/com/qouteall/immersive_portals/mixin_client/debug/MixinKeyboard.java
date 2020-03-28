package com.qouteall.immersive_portals.mixin_client.debug;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Keyboard.class)
public class MixinKeyboard {
    //fix cannot output when taking screenshot
    void method_1464(Text text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
    }
}
