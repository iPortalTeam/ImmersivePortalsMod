package com.qouteall.immersive_portals.mixin;

import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.regex.Pattern;

@Mixin(Language.class)
public interface MixinLanguage {
    @Accessor
    Map<String, String> getTranslations();

    @Accessor
    Pattern getField_11489();
}