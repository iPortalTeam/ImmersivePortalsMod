package com.qouteall.immersive_portals.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Consumer;

public class SelectDimensionScreen extends Screen {
    public final Screen parent;
    private DimListWidget dimListWidget;
    private ButtonWidget confirmButton;
    private Consumer<DimensionType> outerCallback;
    
    protected SelectDimensionScreen(Screen parent, Consumer<DimensionType> callback) {
        super(new TranslatableText("imm_ptl.select_dimension"));
        this.parent = parent;
        this.outerCallback = callback;
    }
    
    @Override
    protected void init() {
        dimListWidget = new DimListWidget(
            width,
            height,
            48,
            height - 64,
            15,
            this
        );
        children.add(dimListWidget);
        
        Consumer<DimTermWidget> callback = w -> dimListWidget.setSelected(w);
        
        Registry.DIMENSION_TYPE.stream().forEach(dimensionType -> {
            dimListWidget.terms.add(
                new DimTermWidget(dimensionType, dimListWidget, callback)
            );
        });
        dimListWidget.update();
        
        confirmButton = (ButtonWidget) this.addButton(new ButtonWidget(
            this.width / 2 - 75, this.height - 28, 150, 20,
            I18n.translate("imm_ptl.confirm_select_dimension"),
            (buttonWidget) -> {
                DimTermWidget selected = dimListWidget.getSelected();
                if (selected == null) {
                    return;
                }
                outerCallback.accept(selected.dimension);
                MinecraftClient.getInstance().openScreen(parent);
            }
        ));
        
    }
    
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        
        dimListWidget.render(mouseX, mouseY, delta);
        
        super.render(mouseX, mouseY, delta);
        
        this.drawCenteredString(
            this.font, this.title.asFormattedString(), this.width / 2, 20, -1
        );
    }
}
