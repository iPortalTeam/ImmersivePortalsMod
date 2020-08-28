package net.logandark.languagehack;

import com.qouteall.hiding_in_the_bushes.O_O;

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
