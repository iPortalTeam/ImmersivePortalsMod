package com.qouteall.immersive_portals.altius_world;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AltiusScreen extends Screen {
    Screen parent;
    private ButtonWidget backButton;
    private ButtonWidget toggleButton;
    private ButtonWidget addDimensionButton;
    private ButtonWidget removeDimensionButton;
    
    private int titleY;
    
    public boolean isEnabled = false;
    private DimListWidget dimListWidget;
    
    public AltiusScreen(Screen parent) {
        super(new TranslatableText("imm_ptl.altius_screen"));
        this.parent = parent;
        
        toggleButton = new ButtonWidget(
            0, 0, 150, 20,
            I18n.translate("imm_ptl.toggle_altius"),
            (buttonWidget) -> {
                setEnabled(!isEnabled);
            }
        );
        
        backButton = new ButtonWidget(
            0, 0, 72, 20,
            I18n.translate("imm_ptl.back_to_create_world"),
            (buttonWidget) -> {
                MinecraftClient.getInstance().openScreen(parent);
            }
        );
        addDimensionButton = new ButtonWidget(
            0, 0, 72, 20,
            I18n.translate("imm_ptl.add_dimension"),
            (buttonWidget) -> {
                onAddDimension();
            }
        );
        removeDimensionButton = new ButtonWidget(
            0, 0, 72, 20,
            I18n.translate("imm_ptl.remove_dimension"),
            (buttonWidget) -> {
                onRemoveDimension();
            }
        );
        
        dimListWidget = new DimListWidget(
            width,
            height,
            100,
            200,
            15,
            this
        );
    
        O_O.registerDimensionsForge();
        
        Consumer<DimTermWidget> callback = getElementSelectCallback();
        dimListWidget.terms.add(
            new DimTermWidget(ModMain.alternate2, dimListWidget, callback)
        );
        dimListWidget.terms.add(
            new DimTermWidget(DimensionType.OVERWORLD, dimListWidget, callback)
        );
        dimListWidget.terms.add(
            new DimTermWidget(DimensionType.THE_NETHER, dimListWidget, callback)
        );
    }
    
    //nullable
    public AltiusInfo getAltiusInfo() {
        if (isEnabled) {
            return new AltiusInfo(
                dimListWidget.terms.stream().map(
                    w -> w.dimension
                ).collect(Collectors.toList())
            );
        }
        else {
            return null;
        }
    }
    
    @Override
    protected void init() {
        
        addButton(toggleButton);
        addButton(backButton);
        addButton(addDimensionButton);
        addButton(removeDimensionButton);
        
        setEnabled(isEnabled);
        
        children.add(dimListWidget);
        
        dimListWidget.update();
        
        CHelper.layout(
            0, height,
            CHelper.LayoutElement.blankSpace(15),
            new CHelper.LayoutElement(true, 20, (from, to) -> {
                titleY = (from + to) / 2;
            }),
            CHelper.LayoutElement.blankSpace(10),
            new CHelper.LayoutElement(true, 20, (a, b) -> {
                toggleButton.x = 20;
                toggleButton.y = a;
            }),
            CHelper.LayoutElement.blankSpace(10),
            new CHelper.LayoutElement(false, 1, (from, to) -> {
                dimListWidget.updateSize(
                    width, height,
                    from, to
                );
            }),
            CHelper.LayoutElement.blankSpace(15),
            new CHelper.LayoutElement(true, 20, (from, to) -> {
                backButton.y = from;
                addDimensionButton.y = from;
                removeDimensionButton.y = from;
                CHelper.layout(
                    0, width,
                    CHelper.LayoutElement.blankSpace(20),
                    CHelper.LayoutElement.layoutX(backButton, 1),
                    CHelper.LayoutElement.blankSpace(10),
                    CHelper.LayoutElement.layoutX(addDimensionButton, 1),
                    CHelper.LayoutElement.blankSpace(10),
                    CHelper.LayoutElement.layoutX(removeDimensionButton, 1),
                    CHelper.LayoutElement.blankSpace(20)
                );
            }),
            CHelper.LayoutElement.blankSpace(15)
        );
    }
    
    private Consumer<DimTermWidget> getElementSelectCallback() {
        return w -> dimListWidget.setSelected(w);
    }
    
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        
        
        if (isEnabled) {
            dimListWidget.render(mouseX, mouseY, delta);
        }
        
        super.render(mouseX, mouseY, delta);
        
        this.drawCenteredString(
            MinecraftClient.getInstance().textRenderer, this.title.asFormattedString(), this.width / 2, 20, -1
        );
        
        
    }
    
    private void setEnabled(boolean cond) {
        isEnabled = cond;
        if (isEnabled) {
            toggleButton.setMessage(I18n.translate("imm_ptl.altius_toggle_true"));
        }
        else {
            toggleButton.setMessage(I18n.translate("imm_ptl.altius_toggle_false"));
        }
        addDimensionButton.visible = isEnabled;
        removeDimensionButton.visible = isEnabled;
    }
    
    private void onAddDimension() {
        DimTermWidget selected = dimListWidget.getSelected();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.terms.indexOf(selected);
        
        if (position < 0 || position > dimListWidget.terms.size()) {
            position = -1;
        }
        
        int insertingPosition = position + 1;
        
        MinecraftClient.getInstance().openScreen(
            new SelectDimensionScreen(
                this,
                dimensionType -> {
                    dimListWidget.terms.add(
                        insertingPosition,
                        new DimTermWidget(
                            dimensionType,
                            dimListWidget,
                            getElementSelectCallback()
                        )
                    );
                    removeDuplicate(insertingPosition);
                    dimListWidget.update();
                }
            )
        );
    }
    
    private void onRemoveDimension() {
        DimTermWidget selected = dimListWidget.getSelected();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.terms.indexOf(selected);
        
        if (position == -1) {
            return;
        }
        
        dimListWidget.terms.remove(position);
        dimListWidget.update();
    }
    
    private void removeDuplicate(int insertedIndex) {
        DimensionType inserted = dimListWidget.terms.get(insertedIndex).dimension;
        for (int i = dimListWidget.terms.size() - 1; i >= 0; i--) {
            if (dimListWidget.terms.get(i).dimension == inserted) {
                if (i != insertedIndex) {
                    dimListWidget.terms.remove(i);
                }
            }
        }
    }
}
