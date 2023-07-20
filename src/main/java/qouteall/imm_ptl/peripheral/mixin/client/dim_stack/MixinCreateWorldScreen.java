package qouteall.imm_ptl.peripheral.mixin.client.dim_stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackGuiController;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimId;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen extends Screen implements IECreateWorldScreen {
    
    @Shadow
    @Final
    private static Logger LOGGER;
    
    @Shadow
    @Final
    private WorldCreationUiState uiState;
    
    @Nullable
    private DimStackGuiController ip_dimStackController;
    
    protected MixinCreateWorldScreen(Component title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onInitEnd(
        Minecraft minecraft, Screen screen, WorldCreationContext worldCreationContext,
        Optional<ResourceKey<WorldPreset>> optional, OptionalLong optionalLong, CallbackInfo ci
    ) {
        DimStackManagement.dimStackToApply = DimStackManagement.getDimStackPreset();
        if (DimStackManagement.dimStackToApply != null) {
            LOGGER.info("[ImmPtl] Applying dimension stack preset");
        }
    }
    
    @Override
    public void ip_openDimStackScreen() {
        if (ip_dimStackController == null) {
            CreateWorldScreen this_ = (CreateWorldScreen) (Object) this;
            ip_dimStackController = new DimStackGuiController(
                this_,
                () -> portal_getDimensionList(),
                info -> {
                    DimStackManagement.dimStackToApply = info;
                    Minecraft.getInstance().setScreen(this_);
                }
            );
            ip_dimStackController.initializeAsDefault();
        }
        
        Minecraft.getInstance().setScreen(ip_dimStackController.view);
    }
    
    private List<ResourceKey<Level>> portal_getDimensionList() {
        Helper.log("Getting the dimension list");
        
        List<ResourceKey<Level>> result = new ArrayList<>();
        
        try {
            WorldCreationContext settings = uiState.getSettings();
            
            RegistryAccess.Frozen registryAccess = settings.worldgenLoadContext();
            
            WorldDimensions selectedDimensions = settings.selectedDimensions();
            
            // add vanilla dimensions
            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : selectedDimensions.dimensions().entrySet()) {
                result.add(DimId.idToKey(entry.getKey().location()));
            }
            
            // add ImmPtl API custom dimensions
            MappedRegistry<LevelStem> customDims = DimensionAPI.collectCustomDimensions(
                settings.worldgenLoadContext(),
                settings.options()
            );
            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : customDims.entrySet()) {
                result.add(DimId.idToKey(entry.getKey().location()));
            }
            
            // add datapack dimensions
            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : settings.datapackDimensions().entrySet()) {
                result.add(DimId.idToKey(entry.getKey().location()));
            }
        }
        catch (Exception e) {
            LOGGER.error("ImmPtl getting dimension list", e);
            if (result.isEmpty()) {
                result.add(DimId.idToKey("error:error"));
            }
        }
        
        return result;
    }
}
