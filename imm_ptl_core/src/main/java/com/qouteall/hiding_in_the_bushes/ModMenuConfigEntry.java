package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
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
            MyConfig currConfig = MyConfig.readConfig();
            
            ConfigBuilder builder = ConfigBuilder.create();
            ConfigCategory serverSide = builder.getOrCreateCategory(
                new TranslatableText("imm_ptl.server_side_config")
            );
            ConfigCategory clientSide = builder.getOrCreateCategory(
                new TranslatableText("imm_ptl.client_side_config")
            );
            IntegerSliderEntry entryMaxPortalLayer = builder.entryBuilder().startIntSlider(
                new TranslatableText("imm_ptl.max_portal_layer"),
                currConfig.maxPortalLayer,
                0, 15
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
            IntegerSliderEntry entryIndirectLoadingRadiusCap = builder.entryBuilder().startIntSlider(
                new TranslatableText("imm_ptl.indirect_loading_radius_cap"),
                currConfig.indirectLoadingRadiusCap,
                1, 32
            ).setDefaultValue(8).build();
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
            ).setDefaultValue(false).build();
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
            BooleanListEntry entryMultiThreadedNetherPortalSearching = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.multi_threaded_nether_portal_searching"),
                currConfig.multiThreadedNetherPortalSearching
            ).setDefaultValue(true).build();
            BooleanListEntry entryMirrorInteractableThroughPortal = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.mirror_interactable_through_portal"),
                currConfig.mirrorInteractableThroughPortal
            ).setDefaultValue(false).build();
            BooleanListEntry entryPureMirror = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.pure_mirror"),
                currConfig.pureMirror
            ).setDefaultValue(false).build();
            BooleanListEntry entryEnableAlternateDimensions = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.enable_alternate_dimensions"),
                currConfig.enableAlternateDimensions
            ).setDefaultValue(true).build();
            BooleanListEntry entryReducedPortalRendering = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.reduced_portal_rendering"),
                currConfig.reducedPortalRendering
            ).setDefaultValue(false).build();
            BooleanListEntry entryLooseMovementCheck = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.loose_movement_check"),
                currConfig.looseMovementCheck
            ).setDefaultValue(false).build();
            BooleanListEntry entryVisibilityPrediction = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.visibility_prediction"),
                currConfig.visibilityPrediction
            ).setDefaultValue(true).build();
            BooleanListEntry entryAutomaticRenderingMerge = builder.entryBuilder().startBooleanToggle(
                new TranslatableText("imm_ptl.automatic_rendering_merge"),
                currConfig.forceMergePortalRendering
            ).setDefaultValue(false).build();
            IntegerSliderEntry entryChunkUnloadDelayTicks = builder.entryBuilder().startIntSlider(
                new TranslatableText("imm_ptl.chunk_unload_delay_ticks"),
                currConfig.chunkUnloadDelayTicks,
                0, 30 * 20
            ).setDefaultValue(15 * 20).build();
            EnumListEntry<Global.NetherPortalMode> entryNetherPortalMode = builder.entryBuilder()
                .startEnumSelector(
                    new TranslatableText("imm_ptl.nether_portal_mode"),
                    Global.NetherPortalMode.class,
                    currConfig.netherPortalMode
                )
                .setDefaultValue(Global.NetherPortalMode.normal)
                .build();
            EnumListEntry<Global.EndPortalMode> entryEndPortalMode = builder.entryBuilder()
                .startEnumSelector(
                    new TranslatableText("imm_ptl.end_portal_mode"),
                    Global.EndPortalMode.class,
                    currConfig.endPortalMode
                )
                .setDefaultValue(Global.EndPortalMode.normal)
                .build();
            StringListListEntry entryDimensionRenderRedirect = builder.entryBuilder().startStrList(
                new TranslatableText("imm_ptl.render_redirect"),
                MyConfig.mapToList(currConfig.dimensionRenderRedirect)
            ).setDefaultValue(MyConfig.defaultRedirectMapList).setInsertInFront(true)
                .setExpanded(true).build();
            clientSide.addEntry(entryMaxPortalLayer);
            clientSide.addEntry(entryLagAttackProof);
            clientSide.addEntry(entryReducedPortalRendering);
            clientSide.addEntry(entryPortalRenderLimit);
            clientSide.addEntry(entryCompatibilityRenderMode);
            clientSide.addEntry(entryVisibilityPrediction);
            clientSide.addEntry(entryAutomaticRenderingMerge);
            clientSide.addEntry(entryCheckGlError);
            clientSide.addEntry(entryPureMirror);
            clientSide.addEntry(entryRenderYourselfInPortal);
            clientSide.addEntry(entryCorrectCrossPortalEntityRendering);
            clientSide.addEntry(entryDimensionRenderRedirect);
            
            serverSide.addEntry(entryIndirectLoadingRadiusCap);
            serverSide.addEntry(entryNetherPortalMode);
            serverSide.addEntry(entryEndPortalMode);
            serverSide.addEntry(entryLongerReachInCreative);
            serverSide.addEntry(entryEnableAlternateDimensions);
            serverSide.addEntry(entryPortalSearchingRange);
            serverSide.addEntry(entryActiveLoading);
            serverSide.addEntry(entryChunkUnloadDelayTicks);
            serverSide.addEntry(entryTeleportDebug);
            serverSide.addEntry(entryLooseMovementCheck);
            serverSide.addEntry(entryMultiThreadedNetherPortalSearching);
            serverSide.addEntry(entryMirrorInteractableThroughPortal);
            
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
                    newConfig.multiThreadedNetherPortalSearching = entryMultiThreadedNetherPortalSearching.getValue();
//                    newConfig.edgelessSky = entryEdgelessSky.getValue();
                    newConfig.mirrorInteractableThroughPortal = entryMirrorInteractableThroughPortal.getValue();
                    newConfig.pureMirror = entryPureMirror.getValue();
                    newConfig.enableAlternateDimensions = entryEnableAlternateDimensions.getValue();
                    newConfig.reducedPortalRendering = entryReducedPortalRendering.getValue();
                    newConfig.indirectLoadingRadiusCap = entryIndirectLoadingRadiusCap.getValue();
                    newConfig.dimensionRenderRedirect = MyConfig.listToMap(
                        entryDimensionRenderRedirect.getValue()
                    );
                    newConfig.netherPortalMode = entryNetherPortalMode.getValue();
                    newConfig.endPortalMode = entryEndPortalMode.getValue();
                    newConfig.looseMovementCheck = entryLooseMovementCheck.getValue();
                    newConfig.visibilityPrediction = entryVisibilityPrediction.getValue();
                    newConfig.chunkUnloadDelayTicks = entryChunkUnloadDelayTicks.getValue();
                    newConfig.forceMergePortalRendering = entryAutomaticRenderingMerge.getValue();
                    
                    newConfig.saveConfigFile();
                    newConfig.onConfigChanged();
                })
                .build();
        };
    }
    
}
