package net.logandark.languagehack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.hiding_in_the_bushes.mixin.MixinLanguage;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Language;
import org.apache.logging.log4j.core.util.Closer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageHack {
    public static void activate(String modid) {
        if (!O_O.isDedicatedServer()) return;
        
//        MixinLanguage language = (MixinLanguage) Language.getInstance();
//        InputStream inputStream = O_O.getLanguageFileStream(modid);
//
//        try {
//            JsonObject jsonObject = new Gson().fromJson(
//                new InputStreamReader(inputStream, StandardCharsets.UTF_8),
//                JsonObject.class
//            );
//
//            jsonObject.entrySet().forEach(entry -> {
//                String string = language.portal_getPattern()
//                    .matcher(JsonHelper.asString(entry.getValue(), entry.getKey()))
//                    .replaceAll("%$1s");
//                language.portal_getTranslations().put(entry.getKey(), string);
//            });
//        }
//        finally {
//            Closer.closeSilently(inputStream);
//        }
    }
}
