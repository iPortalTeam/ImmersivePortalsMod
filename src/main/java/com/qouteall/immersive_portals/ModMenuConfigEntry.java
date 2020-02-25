package com.qouteall.immersive_portals;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;

public class ModMenuConfigEntry implements ModMenuApi {
    @Override
    public String getModId() {
        return "immersive_portals";
    }
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            MyConfigClient currConfig = MyConfigClient.readConfigFromFile();
            
            ConfigBuilder builder = ConfigBuilder.create();
            ConfigCategory category = builder.getOrCreateCategory("imm_ptl.main_category");
            IntegerSliderEntry entryMaxPortalLayer = builder.entryBuilder().startIntSlider(
                "imm_ptl.max_portal_layer",
                currConfig.maxPortalLayer,
                1, 15
            ).setDefaultValue(5).build();
            BooleanListEntry entryCompatibilityRenderMode = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.compatibility_render_mode",
                currConfig.compatibilityRenderMode
            ).setDefaultValue(false).build();
            BooleanListEntry entryCheckGlError = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.check_gl_error",
                currConfig.doCheckGlError
            ).setDefaultValue(false).build();
            category.addEntry(entryMaxPortalLayer);
            category.addEntry(entryCompatibilityRenderMode);
            category.addEntry(entryCheckGlError);
            return builder
                .setParentScreen(parent)
                .setSavingRunnable(() -> {
                    MyConfigClient newConfigObject = new MyConfigClient();
                    newConfigObject.maxPortalLayer = entryMaxPortalLayer.getValue();
                    newConfigObject.compatibilityRenderMode = entryCompatibilityRenderMode.getValue();
                    newConfigObject.doCheckGlError = entryCheckGlError.getValue();
                    MyConfigClient.saveConfigFile(newConfigObject);
                    MyConfigClient.onConfigChanged(newConfigObject);
                })
                .build();
        };
    }
}
