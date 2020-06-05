package com.qouteall.hiding_in_the_bushes.mixin;

import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.regex.Pattern;

@Mixin(Language.class)
public interface MixinLanguage {
    @Accessor("translations")
    Map<String, String> portal_getTranslations();

    @Accessor("field_11489")
    Pattern portal_getPattern();
}