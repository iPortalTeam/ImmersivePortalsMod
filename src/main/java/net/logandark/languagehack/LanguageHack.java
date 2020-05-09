package net.logandark.languagehack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.mixin.MixinLanguage;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Language;
import org.apache.logging.log4j.core.util.Closer;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageHack {
    public static void activate(String modid) {
        if (!O_O.isDedicatedServer()) return;

        MixinLanguage language = (MixinLanguage) Language.getInstance();
        FileInputStream inputStream = O_O.getLanguageFileStream(modid);

        try {
            JsonObject jsonObject = new Gson().fromJson(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
                JsonObject.class
            );

            jsonObject.entrySet().forEach(entry -> {
                String string = language.getField_11489()
                    .matcher(JsonHelper.asString(entry.getValue(), entry.getKey()))
                    .replaceAll("%$1s");
                language.getTranslations().put(entry.getKey(), string);
            });
        } finally {
            Closer.closeSilently(inputStream);
        }
    }
}
