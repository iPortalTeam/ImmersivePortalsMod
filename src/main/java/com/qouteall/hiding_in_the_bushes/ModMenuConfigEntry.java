package com.qouteall.hiding_in_the_bushes;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;

public class ModMenuConfigEntry implements ModMenuApi {
    
    
    @Override
    public String getModId() {
        return "immersive_portals";
    }
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            MyConfig currConfig = MyConfig.readConfigFromFile();
            
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
            IntegerSliderEntry entryPortalSearchingRange = builder.entryBuilder().startIntSlider(
                "imm_ptl.portal_searching_range",
                currConfig.portalSearchingRange,
                32, 1000
            ).setDefaultValue(128).build();
            BooleanListEntry entryLongerReachInCreative = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.long_reach_in_creative",
                currConfig.longerReachInCreative
            ).setDefaultValue(true).build();
            BooleanListEntry entryRenderYourselfInPortal = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.render_yourself_in_portal",
                currConfig.renderYourselfInPortal
            ).setDefaultValue(true).build();
            BooleanListEntry entryActiveLoading = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.active_loading",
                currConfig.activeLoading
            ).setDefaultValue(true).build();
            BooleanListEntry entryTeleportDebug = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.teleportation_debug",
                currConfig.teleportationDebug
            ).setDefaultValue(false).build();
            BooleanListEntry entryCorrectCrossPortalEntityRendering = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.correct_cross_portal_entity_rendering",
                currConfig.correctCrossPortalEntityRendering
            ).setDefaultValue(true).build();
            BooleanListEntry entryLoadFewerChunks = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.load_fewer_chunks",
                currConfig.loadFewerChunks
            ).setDefaultValue(false).build();
            BooleanListEntry entryMultiThreadedNetherPortalSearching = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.multi_threaded_nether_portal_searching",
                currConfig.multiThreadedNetherPortalSearching
            ).setDefaultValue(true).build();
            BooleanListEntry entryEdgelessSky = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.edgeless_sky",
                currConfig.edgelessSky
            ).setDefaultValue(false).build();
            BooleanListEntry entryReversibleNetherPortalLinking = builder.entryBuilder().startBooleanToggle(
                "imm_ptl.reversible_nether_portal_linking",
                currConfig.reversibleNetherPortalLinking
            ).setDefaultValue(false).build();
            BooleanListEntry entryMirrorInteractableThroughPortal = builder.entryBuilder().startBooleanToggle(
                    "imm_ptl.mirror_interactable_through_portal",
                    currConfig.mirrorInteractableThroughPortal
            ).setDefaultValue(true).build();
            StringListListEntry entryDimensionRenderRedirect = builder.entryBuilder().startStrList(
                "imm_ptl.render_redirect",
                MyConfig.mapToList(currConfig.dimensionRenderRedirect)
            ).setDefaultValue(MyConfig.defaultRedirectMapList).setInsertInFront(true).setExpanded(
                true).build();
            category.addEntry(entryMaxPortalLayer);
            category.addEntry(entryCompatibilityRenderMode);
            category.addEntry(entryCheckGlError);
            category.addEntry(entryPortalSearchingRange);
            category.addEntry(entryLongerReachInCreative);
            category.addEntry(entryRenderYourselfInPortal);
            category.addEntry(entryActiveLoading);
            category.addEntry(entryTeleportDebug);
            category.addEntry(entryCorrectCrossPortalEntityRendering);
            category.addEntry(entryLoadFewerChunks);
            category.addEntry(entryMultiThreadedNetherPortalSearching);
            category.addEntry(entryEdgelessSky);
            category.addEntry(entryReversibleNetherPortalLinking);
            category.addEntry(entryMirrorInteractableThroughPortal);
            category.addEntry(entryDimensionRenderRedirect);
            return builder
                .setParentScreen(parent)
                .setSavingRunnable(() -> {
                    MyConfig newConfigObject = new MyConfig();
                    newConfigObject.maxPortalLayer = entryMaxPortalLayer.getValue();
                    newConfigObject.compatibilityRenderMode = entryCompatibilityRenderMode.getValue();
                    newConfigObject.doCheckGlError = entryCheckGlError.getValue();
                    newConfigObject.portalSearchingRange = entryPortalSearchingRange.getValue();
                    newConfigObject.longerReachInCreative = entryLongerReachInCreative.getValue();
                    newConfigObject.renderYourselfInPortal = entryRenderYourselfInPortal.getValue();
                    newConfigObject.activeLoading = entryActiveLoading.getValue();
                    newConfigObject.teleportationDebug = entryTeleportDebug.getValue();
                    newConfigObject.correctCrossPortalEntityRendering = entryCorrectCrossPortalEntityRendering.getValue();
                    newConfigObject.loadFewerChunks = entryLoadFewerChunks.getValue();
                    newConfigObject.multiThreadedNetherPortalSearching = entryMultiThreadedNetherPortalSearching.getValue();
                    newConfigObject.edgelessSky = entryEdgelessSky.getValue();
                    newConfigObject.reversibleNetherPortalLinking = entryReversibleNetherPortalLinking.getValue();
                    newConfigObject.mirrorInteractableThroughPortal = entryMirrorInteractableThroughPortal.getValue();
                    newConfigObject.dimensionRenderRedirect = MyConfig.listToMap(
                        entryDimensionRenderRedirect.getValue()
                    );
                    newConfigObject.saveConfigFile();
                    newConfigObject.onConfigChanged();
                })
                .build();
        };
    }
    
}
