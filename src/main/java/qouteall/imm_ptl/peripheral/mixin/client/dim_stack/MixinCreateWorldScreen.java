package qouteall.imm_ptl.peripheral.mixin.client.dim_stack;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
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
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackInfo;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackScreen;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.mixin.dimension.IELayeredRegistryAccess;

import javax.annotation.Nullable;
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
    private DimStackScreen ip_dimStackScreen;
    
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
        DimStackManagement.dimStackToApply = IPOuterClientMisc.getDimStackPreset();
        if (DimStackManagement.dimStackToApply != null) {
            LOGGER.info("[ImmPtl] Applying dimension stack preset");
        }
    }
    
    @Override
    public void ip_openDimStackScreen() {
        if (ip_dimStackScreen == null) {
            ip_dimStackScreen = new DimStackScreen(
                (CreateWorldScreen) (Object) this,
                this::portal_getDimensionList,
                info -> {
                    DimStackManagement.dimStackToApply = info;
                }
            );
        }
        
        Minecraft.getInstance().setScreen(ip_dimStackScreen);
    }
    
    private List<ResourceKey<Level>> portal_getDimensionList(Screen addDimensionScreen) {
        Helper.log("Getting the dimension list");
        
        try {
            WorldCreationContext settings = uiState.getSettings();
            
            RegistryAccess.Frozen registryAccess = settings.worldgenLoadContext();
            
            WorldDimensions selectedDimensions = settings.selectedDimensions();
            
            MappedRegistry<LevelStem> subDimensionRegistry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
            
            // add vanilla dimensions
            for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : selectedDimensions.dimensions().entrySet()) {
                subDimensionRegistry.register(entry.getKey(), entry.getValue(), Lifecycle.stable());
            }
            
            RegistryAccess.Frozen subRegistryAccess =
                new RegistryAccess.ImmutableRegistryAccess(List.of(subDimensionRegistry)).freeze();
            
            LayeredRegistryAccess<Integer> wrappedLayeredRegistryAccess = IELayeredRegistryAccess.ip_init(
                List.of(1, 2),
                List.of(registryAccess, subRegistryAccess)
            );
            RegistryAccess.Frozen wrappedRegistryAccess = wrappedLayeredRegistryAccess.compositeAccess();
            
            DimensionAPI.serverDimensionsLoadEvent.invoker().run(settings.options(), wrappedRegistryAccess);
            
            return subDimensionRegistry
                .keySet().stream().map(DimId::idToKey).toList();
        }
        catch (Exception e) {
            LOGGER.error("ImmPtl getting dimension list", e);
            return List.of(DimId.idToKey("error:error"));
        }
    }
}
