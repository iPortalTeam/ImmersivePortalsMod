package qouteall.imm_ptl.peripheral.mixin.client.altius_world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.dim_sync.DimId;
import qouteall.imm_ptl.peripheral.altius_world.AltiusGameRule;
import qouteall.imm_ptl.peripheral.altius_world.AltiusInfo;
import qouteall.imm_ptl.peripheral.altius_world.AltiusManagement;
import qouteall.imm_ptl.peripheral.altius_world.AltiusScreen;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.DimensionAPI;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

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
    private Button altiusButton;
    
    @Nullable
    private AltiusScreen altiusScreen;
    
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
        
        altiusButton = (Button) this.addRenderableWidget(new Button(
            width / 2 + 5, 151, 150, 20,
            new TranslatableComponent("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openAltiusScreen();
            }
        ));
        altiusButton.visible = false;
        
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;setWorldGenSettingsVisible(Z)V",
        at = @At("RETURN")
    )
    private void onMoreOptionsOpen(boolean moreOptionsOpen, CallbackInfo ci) {
        if (moreOptionsOpen) {
            altiusButton.visible = true;
        }
        else {
            altiusButton.visible = false;
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
        if (altiusScreen != null) {
            AltiusInfo info = altiusScreen.getAltiusInfo();
            
            if (info != null) {
                AltiusManagement.dimStackToApply = info;
                
                GameRules.BooleanValue rule = levelInfo.gameRules().getRule(AltiusGameRule.dimensionStackKey);
                rule.set(true, null);
                
                Helper.log("Generating dimension stack world");
            }
        }
        
        client.createLevel(resultFolder, levelInfo, registryTracker, generatorOptions);
    }
    
    private void openAltiusScreen() {
        if (altiusScreen == null) {
            altiusScreen = new AltiusScreen(
                (CreateWorldScreen) (Object) this,
                this::portal_getDimensionList,
                a -> {
                    // clicking "Finish" invokes nothing
                }
            );
        }
        
        Minecraft.getInstance().setScreen(altiusScreen);
    }
    
    private List<ResourceKey<Level>> portal_getDimensionList() {
        WorldGenSettings rawGeneratorOptions = worldGenSettingsComponent.makeSettings(false);
        
        RegistryAccess registryManager = worldGenSettingsComponent.registryHolder();

//        WorldGenSettings populated = WorldCreationDimensionHelper.populateGeneratorOptions1(
//            rawGeneratorOptions, registryManager,
//            portal_getResourcePackManager(),
//            dataPacks
//        );
        
        // TODO check it
        WorldGenSettings populated = rawGeneratorOptions;
        
        
        // register the alternate dimensions
        DimensionAPI.serverDimensionsLoadEvent.invoker().run(populated, registryManager);
        
        return populated.dimensions().entrySet().stream().map(
            e -> DimId.idToKey(e.getKey().location())
        ).collect(Collectors.toList());
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
