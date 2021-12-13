package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class SelectDimensionScreen extends Screen {
    public final AltiusScreen parent;
    private DimListWidget dimListWidget;
    private ButtonWidget confirmButton;
    private Consumer<RegistryKey<World>> outerCallback;
    
    protected SelectDimensionScreen(AltiusScreen parent, Consumer<RegistryKey<World>> callback) {
        super(new TranslatableText("imm_ptl.select_dimension"));
        this.parent = parent;
        this.outerCallback = callback;
    }
    
//    public static List<RegistryKey<World>> getDimensionList(
//        Supplier<GeneratorOptions> generatorOptionsSupplier,
//        DynamicRegistryManager.Impl dynamicRegistryManager
//    ) {
//
//        GeneratorOptions generatorOptions = generatorOptionsSupplier.get();
//        SimpleRegistry<DimensionOptions> dimensionMap = generatorOptions.getDimensions();
//
//        DimensionAPI.serverDimensionsLoadEvent.invoker().run(generatorOptions, dynamicRegistryManager);
//
//        ArrayList<RegistryKey<World>> dimList = new ArrayList<>();
//
//        for (Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> entry : dimensionMap.getEntries()) {
//            dimList.add(RegistryKey.of(Registry.WORLD_KEY, entry.getKey().getValue()));
//        }
//
//        return dimList;
//    }
    
    @Override
    protected void init() {
        dimListWidget = new DimListWidget(
            width,
            height,
            20,
            height - 30,
            DimEntryWidget.widgetHeight,
            this,
            DimListWidget.Type.addDimensionList
        );
        addSelectableChild(dimListWidget);
        
        Consumer<DimEntryWidget> callback = w -> dimListWidget.setSelected(w);
        
        for (RegistryKey<World> dim : parent.dimensionListSupplier.get()) {
            dimListWidget.entryWidgets.add(new DimEntryWidget(dim, dimListWidget, callback, DimEntryWidget.Type.simple));
        }
        
        dimListWidget.update();
        
        confirmButton = (ButtonWidget) addDrawableChild(new ButtonWidget(
            this.width / 2 - 75, this.height - 28, 150, 20,
            new TranslatableText("imm_ptl.confirm_select_dimension"),
            (buttonWidget) -> {
                DimEntryWidget selected = dimListWidget.getSelectedOrNull();
                if (selected == null) {
                    return;
                }
                outerCallback.accept(selected.dimension);
                MinecraftClient.getInstance().setScreen(parent);
            }
        ));
        
    }
    
    @Override
    public void onClose() {
        // When `esc` is pressed return to the parent screen rather than setting screen to `null` which returns to the main menu.
        this.client.setScreen(this.parent);
    }
    
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrixStack);
        
        dimListWidget.render(matrixStack, mouseX, mouseY, delta);
        
        super.render(matrixStack, mouseX, mouseY, delta);
        
        this.drawCenteredText(
            matrixStack, this.textRenderer, this.title.asString(), this.width / 2, 20, -1
        );
    }
}
