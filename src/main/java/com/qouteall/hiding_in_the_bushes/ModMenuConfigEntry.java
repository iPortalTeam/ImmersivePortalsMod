package com.qouteall.hiding_in_the_bushes;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.text.TranslatableText;

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
            ConfigCategory category = builder.getOrCreateCategory(
                new TranslatableText("imm_ptl.main_category")
            );
            IntegerSliderEntry entryMaxPortalLayer = builder.entryBuilder().startIntSlider(
                new TranslatableText("imm_ptl.max_portal_layer"),
                currConfig.maxPortalLayer,
                1, 15
            ).setDefaultValue(5).build();
            BooleanListEntry entryLagAttackProof = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.lag_attack_proof"),
                currConfig.lagAttackProof
            ).setDefaultValue(true).build();
            IntegerSliderEntry entryPortalRenderLimit = builder.entryBuilder().startIntSlider(
                new TranslatableText("imm_ptl.portal_render_limit"),
                currConfig.portalRenderLimit,
                0, 1000
            ).setDefaultValue(200).build();
            BooleanListEntry entryCompatibilityRenderMode = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.compatibility_render_mode"),
                currConfig.compatibilityRenderMode
            ).setDefaultValue(false).build();
            BooleanListEntry entryCheckGlError = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.check_gl_error"),
                currConfig.doCheckGlError
            ).setDefaultValue(false).build();
            IntegerSliderEntry entryPortalSearchingRange = builder.entryBuilder().startIntSlider(
                new TranslatableText("imm_ptl.portal_searching_range"),
                currConfig.portalSearchingRange,
                32, 1000
            ).setDefaultValue(128).build();
            BooleanListEntry entryLongerReachInCreative = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.long_reach_in_creative"),
                currConfig.longerReachInCreative
            ).setDefaultValue(true).build();
            BooleanListEntry entryRenderYourselfInPortal = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.render_yourself_in_portal"),
                currConfig.renderYourselfInPortal
            ).setDefaultValue(true).build();
            BooleanListEntry entryActiveLoading = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.active_loading"),
                currConfig.activeLoading
            ).setDefaultValue(true).build();
            BooleanListEntry entryTeleportDebug = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.teleportation_debug"),
                currConfig.teleportationDebug
            ).setDefaultValue(false).build();
            BooleanListEntry entryCorrectCrossPortalEntityRendering = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.correct_cross_portal_entity_rendering"),
                currConfig.correctCrossPortalEntityRendering
            ).setDefaultValue(true).build();
            BooleanListEntry entryLoadFewerChunks = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.load_fewer_chunks"),
                currConfig.loadFewerChunks
            ).setDefaultValue(false).build();
            BooleanListEntry entryMultiThreadedNetherPortalSearching = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.multi_threaded_nether_portal_searching"),
                currConfig.multiThreadedNetherPortalSearching
            ).setDefaultValue(true).build();
            BooleanListEntry entryEdgelessSky = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.edgeless_sky"),
                currConfig.edgelessSky
            ).setDefaultValue(false).build();
            BooleanListEntry entryReversibleNetherPortalLinking = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.reversible_nether_portal_linking"),
                currConfig.reversibleNetherPortalLinking
            ).setDefaultValue(false).build();
            BooleanListEntry entryMirrorInteractableThroughPortal = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.mirror_interactable_through_portal"),
                currConfig.mirrorInteractableThroughPortal
            ).setDefaultValue(false).build();
            BooleanListEntry entryPureMirror = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.pure_mirror"),
                currConfig.pureMirror
            ).setDefaultValue(false).build();
            StringListListEntry entryDimensionRenderRedirect = builder.entryBuilder().startStrList(
                new TranslatableText("imm_ptl.render_redirect"),
                MyConfig.mapToList(currConfig.dimensionRenderRedirect)
            ).setDefaultValue(MyConfig.defaultRedirectMapList).setInsertInFront(true)
                .setExpanded(true).build();
            StringListListEntry entryPortalGeneration = builder.entryBuilder().startStrList(
                new TranslatableText("imm_ptl.portal_gen"),
                currConfig.customizedPortalGeneration
            ).setDefaultValue(MyConfig.defaultPortalGenList).setInsertInFront(true)
                .setExpanded(true).build();
            category.addEntry(entryMaxPortalLayer);
            category.addEntry(entryLagAttackProof);
            category.addEntry(entryPortalRenderLimit);
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
            category.addEntry(entryPureMirror);
            category.addEntry(entryDimensionRenderRedirect);
            category.addEntry(entryPortalGeneration);
            return builder
                .setParentScreen(parent)
                .setSavingRunnable(() -> {
                    MyConfig newConfig = new MyConfig();
                    newConfig.maxPortalLayer = entryMaxPortalLayer.getValue();
                    newConfig.lagAttackProof = entryLagAttackProof.getValue();
                    newConfig.portalRenderLimit = entryPortalRenderLimit.getValue();
                    newConfig.compatibilityRenderMode = entryCompatibilityRenderMode.getValue();
                    newConfig.doCheckGlError = entryCheckGlError.getValue();
                    newConfig.portalSearchingRange = entryPortalSearchingRange.getValue();
                    newConfig.longerReachInCreative = entryLongerReachInCreative.getValue();
                    newConfig.renderYourselfInPortal = entryRenderYourselfInPortal.getValue();
                    newConfig.activeLoading = entryActiveLoading.getValue();
                    newConfig.teleportationDebug = entryTeleportDebug.getValue();
                    newConfig.correctCrossPortalEntityRendering = entryCorrectCrossPortalEntityRendering.getValue();
                    newConfig.loadFewerChunks = entryLoadFewerChunks.getValue();
                    newConfig.multiThreadedNetherPortalSearching = entryMultiThreadedNetherPortalSearching.getValue();
                    newConfig.edgelessSky = entryEdgelessSky.getValue();
                    newConfig.reversibleNetherPortalLinking = entryReversibleNetherPortalLinking.getValue();
                    newConfig.mirrorInteractableThroughPortal = entryMirrorInteractableThroughPortal.getValue();
                    newConfig.pureMirror = entryPureMirror.getValue();
                    newConfig.dimensionRenderRedirect = MyConfig.listToMap(
                        entryDimensionRenderRedirect.getValue()
                    );
                    newConfig.customizedPortalGeneration = entryPortalGeneration.getValue();
                    newConfig.saveConfigFile();
                    newConfig.onConfigChanged();
                })
                .build();
        };
    }
    
}
