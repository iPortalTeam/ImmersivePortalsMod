package qouteall.imm_ptl.peripheral.mixin.client.dim_stack;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackInfo;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackScreen;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.List;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen extends Screen implements IECreateWorldScreen {
    @Shadow
    public abstract void removed();
    
    @Shadow
    protected DataPackConfig dataPacks;
    
    @Shadow
    @org.jetbrains.annotations.Nullable
    protected abstract Pair<File, PackRepository> getDataPackSelectionSettings();
    
    @Shadow
    @Final
    public WorldGenSettingsComponent worldGenSettingsComponent;
    
    @Shadow
    protected abstract void tryApplyNewDataPacks(PackRepository repository);
    
    private Button dimStackButton;
    
    @Nullable
    private DimStackScreen ip_dimStackScreen;
    
    @Nullable
    private WorldGenSettings ip_lastWorldGenSettings;
    
    @Nullable
    private RegistryAccess ip_lastRegistryAccess;
    
    protected MixinCreateWorldScreen(Component title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/world/level/DataPackConfig;Lnet/minecraft/client/gui/screens/worldselection/WorldGenSettingsComponent;)V",
        at = @At("RETURN")
    )
    private void onConstructEnded(
        Screen screen, DataPackConfig dataPackSettings, WorldGenSettingsComponent moreOptionsDialog,
        CallbackInfo ci
    ) {
    
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;init()V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        dimStackButton = (Button) this.addRenderableWidget(new Button(
            width / 2 + 5, 151, 150, 20,
            new TranslatableComponent("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openDimStackScreen();
            }
        ));
        dimStackButton.visible = false;
        
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;setWorldGenSettingsVisible(Z)V",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            dimStackButton.visible = true;
        }
        else {
            dimStackButton.visible = false;
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;onCreate()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;createLevel(Ljava/lang/String;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/levelgen/WorldGenSettings;)V"
        )
    )
    private void redirectOnCreateLevel(
        Minecraft client, String resultFolder, LevelSettings levelInfo,
        RegistryAccess registryTracker, WorldGenSettings generatorOptions
    ) {
        if (ip_dimStackScreen != null) {
            DimStackInfo info = ip_dimStackScreen.getDimStackInfo();
            
            if (info != null) {
                DimStackManagement.dimStackToApply = info;
                
                Helper.log("Generating dimension stack world");
            }
        }
        
        client.createLevel(resultFolder, levelInfo, registryTracker, generatorOptions);
    }
    
    
    @Inject(
        method = "method_40209",
        at = @At("RETURN")
    )
    private void onTryingApplyNewDatapackLoading(ResourceManager resourceManager, DataPackConfig dataPackConfig, CallbackInfoReturnable<Pair> cir) {
        Pair<PrimaryLevelData, RegistryAccess> returnValue = cir.getReturnValue();
        
        PrimaryLevelData primaryLevelData = returnValue.getFirst();
        
        ip_lastWorldGenSettings = primaryLevelData.worldGenSettings();
        
        ip_lastRegistryAccess = returnValue.getSecond();
    }
    
    private void openDimStackScreen() {
        if (ip_dimStackScreen == null) {
            ip_dimStackScreen = new DimStackScreen(
                (CreateWorldScreen) (Object) this,
                this::portal_getDimensionList,
                a -> {
                    // clicking "Finish" invokes nothing
                }
            );
        }
        
        Minecraft.getInstance().setScreen(ip_dimStackScreen);
    }
    
    private List<ResourceKey<Level>> portal_getDimensionList(Screen addDimensionScreen) {
        Helper.log("Getting the dimension list");
        
        if (ip_lastWorldGenSettings == null) {
            // (MC 1.18.2) it will load the pack in render thread
            tryApplyNewDataPacks(minecraft.getResourcePackRepository());
            
            // it will switch to the create world screen, switch back
            IPGlobal.preTotalRenderTaskList.addTask(() -> {
                if (minecraft.screen == this) {
                    minecraft.setScreen(addDimensionScreen);
                    return true;
                }
                return false;
            });
        }
        
        // this won't contain custom dimensions
        WorldGenSettings rawGeneratorOptions = worldGenSettingsComponent.makeSettings(false);
        
        WorldGenSettings copiedGeneratorOptions = new WorldGenSettings(
            rawGeneratorOptions.seed(), rawGeneratorOptions.generateFeatures(),
            rawGeneratorOptions.generateBonusChest(),
            MiscHelper.filterAndCopyRegistry(
                ((MappedRegistry<LevelStem>) rawGeneratorOptions.dimensions()),
                (a, b) -> true
            )
        );
        
        try {
            // register custom dimensions including alternate dimensions
            if (ip_lastRegistryAccess != null) {
                DimensionAPI.serverDimensionsLoadEvent.invoker().run(copiedGeneratorOptions, ip_lastRegistryAccess);
            }
            else {
                Helper.err("Null registry access");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        HashSet<ResourceKey<Level>> dims = new HashSet<>();
        
        if (ip_lastWorldGenSettings != null) {
            ip_lastWorldGenSettings.dimensions().keySet().forEach(id -> {
                dims.add(DimId.idToKey(id));
            });
        }
        else {
            Helper.err("Null WorldGen settings");
        }
        
        copiedGeneratorOptions.dimensions().keySet().forEach(id -> {
            dims.add(DimId.idToKey(id));
        });
        
        return dims.stream().toList();
    }
    
    @Override
    public PackRepository portal_getResourcePackManager() {
        return getDataPackSelectionSettings().getSecond();
    }
    
    @Override
    public DataPackConfig portal_getDataPackSettings() {
        return dataPacks;
    }
}
