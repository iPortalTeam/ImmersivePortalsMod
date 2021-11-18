package qouteall.imm_ptl.peripheral.mixin.client.altius_world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.peripheral.altius_world.AltiusGameRule;
import qouteall.imm_ptl.peripheral.altius_world.AltiusInfo;
import qouteall.imm_ptl.peripheral.altius_world.AltiusManagement;
import qouteall.imm_ptl.peripheral.altius_world.AltiusScreen;
import qouteall.imm_ptl.peripheral.altius_world.WorldCreationDimensionHelper;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    protected DataPackSettings dataPackSettings;
    
    @Shadow
    @org.jetbrains.annotations.Nullable
    protected abstract Pair<File, ResourcePackManager> getScannedPack();
    
    @Shadow
    @Final
    public MoreOptionsDialog moreOptionsDialog;
    private ButtonWidget altiusButton;
    
    @Nullable
    private AltiusScreen altiusScreen;
    
    protected MixinCreateWorldScreen(Text title) {
        super(title);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/resource/DataPackSettings;Lnet/minecraft/client/gui/screen/world/MoreOptionsDialog;)V",
        at = @At("RETURN")
    )
    private void onConstructEnded(
        Screen screen, DataPackSettings dataPackSettings, MoreOptionsDialog moreOptionsDialog,
        CallbackInfo ci
    ) {
    
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;init()V",
        at = @At("HEAD")
    )
    private void onInitEnded(CallbackInfo ci) {
        
        altiusButton = (ButtonWidget) this.addDrawableChild(new ButtonWidget(
            width / 2 + 5, 151, 150, 20,
            new TranslatableText("imm_ptl.altius_screen_button"),
            (buttonWidget) -> {
                openAltiusScreen();
            }
        ));
        altiusButton.visible = false;
        
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;setMoreOptionsOpen(Z)V",
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
        method = "createLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;createWorld(Ljava/lang/String;Lnet/minecraft/world/level/LevelInfo;Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;Lnet/minecraft/world/gen/GeneratorOptions;)V"
        )
    )
    private void redirectOnCreateLevel(
        MinecraftClient client, String worldName, LevelInfo levelInfo,
        DynamicRegistryManager.Impl registryTracker, GeneratorOptions generatorOptions
    ) {
        if (altiusScreen != null) {
            AltiusInfo info = altiusScreen.getAltiusInfo();
            
            if (info != null) {
                AltiusManagement.dimensionStackPortalsToGenerate = info;
                
                GameRules.BooleanRule rule = levelInfo.getGameRules().get(AltiusGameRule.dimensionStackKey);
                rule.set(true, null);
                
                Helper.log("Generating dimension stack world");
            }
        }
        
        client.createWorld(worldName, levelInfo, registryTracker, generatorOptions);
    }
    
    private void openAltiusScreen() {
        if (altiusScreen == null) {
            altiusScreen = new AltiusScreen(
                (CreateWorldScreen) (Object) this,
                this::portal_getDimensionList
            );
        }
        
        MinecraftClient.getInstance().setScreen(altiusScreen);
    }
    
    private List<RegistryKey<World>> portal_getDimensionList() {
        GeneratorOptions rawGeneratorOptions = moreOptionsDialog.getGeneratorOptions(false);
        
        DynamicRegistryManager.Impl registryManager = moreOptionsDialog.getRegistryManager();
        
        GeneratorOptions populated = WorldCreationDimensionHelper.populateGeneratorOptions1(
            rawGeneratorOptions, registryManager,
            portal_getResourcePackManager(),
            dataPackSettings
        );
        
        // register the alternate dimensions
        DimensionAPI.serverDimensionsLoadEvent.invoker().run(populated, registryManager);
        
        return populated.getDimensions().getEntries().stream().map(
            e -> DimId.idToKey(e.getKey().getValue())
        ).collect(Collectors.toList());
    }
    
    @Override
    public ResourcePackManager portal_getResourcePackManager() {
        return getScannedPack().getSecond();
    }
    
    @Override
    public DataPackSettings portal_getDataPackSettings() {
        return dataPackSettings;
    }
}
