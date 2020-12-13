package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.imm_ptl_peripheral.alternate_dimension.AlternateDimensions;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.gen.GeneratorOptions;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class AltiusScreen extends Screen {
    CreateWorldScreen parent;
    private final ButtonWidget backButton;
    private final ButtonWidget toggleButton;
    private final ButtonWidget addDimensionButton;
    private final ButtonWidget removeDimensionButton;
    
    private final ButtonWidget respectSpaceRatioButton;
    private final ButtonWidget loopButton;
    
    private final ButtonWidget helpButton;
    
    private int titleY;
    
    public boolean isEnabled = false;
    private final DimListWidget dimListWidget;
    private final Supplier<GeneratorOptions> generatorOptionsSupplier1;
    
    private boolean respectSpaceRatio = false;
    private boolean loop = false;
    
    public AltiusScreen(CreateWorldScreen parent) {
        super(new TranslatableText("imm_ptl.altius_screen"));
        this.parent = parent;
        
        toggleButton = new ButtonWidget(
            0, 0, 150, 20,
            new TranslatableText("imm_ptl.toggle_altius"),
            (buttonWidget) -> {
                setEnabled(!isEnabled);
            }
        );
        
        backButton = new ButtonWidget(
            0, 0, 72, 20,
            new TranslatableText("imm_ptl.back_to_create_world"),
            (buttonWidget) -> {
                MinecraftClient.getInstance().openScreen(parent);
            }
        );
        addDimensionButton = new ButtonWidget(
            0, 0, 72, 20,
            new TranslatableText("imm_ptl.add_dimension"),
            (buttonWidget) -> {
                onAddDimension();
            }
        );
        removeDimensionButton = new ButtonWidget(
            0, 0, 72, 20,
            new TranslatableText("imm_ptl.remove_dimension"),
            (buttonWidget) -> {
                onRemoveDimension();
            }
        );
        
        respectSpaceRatioButton = new ButtonWidget(
            0, 0, 72, 20,
            new TranslatableText("imm_ptl.respect_space_ratio_disabled"),
            (buttonWidget) -> {
                respectSpaceRatio = !respectSpaceRatio;
                buttonWidget.setMessage(new TranslatableText(
                    respectSpaceRatio ?
                        "imm_ptl.respect_space_ratio_enabled" : "imm_ptl.respect_space_ratio_disabled"
                ));
            }
        );
        
        loopButton = new ButtonWidget(
            0, 0, 72, 20,
            new TranslatableText("imm_ptl.loop_disabled"),
            (buttonWidget) -> {
                loop = !loop;
                buttonWidget.setMessage(new TranslatableText(
                    loop ?
                        "imm_ptl.loop_enabled" : "imm_ptl.loop_disabled"
                ));
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
        
        Consumer<DimTermWidget> callback = getElementSelectCallback();
        if (Global.enableAlternateDimensions) {
            dimListWidget.terms.add(
                new DimTermWidget(AlternateDimensions.alternate5, dimListWidget, callback)
            );
            dimListWidget.terms.add(
                new DimTermWidget(AlternateDimensions.alternate2, dimListWidget, callback)
            );
        }
        dimListWidget.terms.add(
            new DimTermWidget(World.OVERWORLD, dimListWidget, callback)
        );
        dimListWidget.terms.add(
            new DimTermWidget(World.NETHER, dimListWidget, callback)
        );
        
        generatorOptionsSupplier1 = Helper.cached(() -> {
            GeneratorOptions rawGeneratorOptions =
                this.parent.moreOptionsDialog.getGeneratorOptions(false);
            return WorldCreationDimensionHelper.getPopulatedGeneratorOptions(
                this.parent, rawGeneratorOptions
            );
        });
        
        helpButton = new ButtonWidget(
            0, 0, 72, 20,
            new LiteralText("?"),
            button -> {
                CHelper.openLinkConfirmScreen(
                    this, "https://qouteall.fun/immptl/wiki/Dimension-Stack"
                );
            }
        );
    }
    
    //nullable
    public AltiusInfo getAltiusInfo() {
        if (isEnabled) {
            return new AltiusInfo(
                dimListWidget.terms.stream().map(
                    w -> w.dimension
                ).collect(Collectors.toList()),
                loop, respectSpaceRatio
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
        
        addButton(loopButton);
        addButton(respectSpaceRatioButton);
        
        addButton(helpButton);
        
        setEnabled(isEnabled);
        
        children.add(dimListWidget);
        
        dimListWidget.update();
        
        CHelper.layout(
            0, height,
            CHelper.LayoutElement.blankSpace(15),
            new CHelper.LayoutElement(true, 20, (from, to) -> {
                helpButton.x = width - 50;
                helpButton.y = 10;
                helpButton.setWidth(30);
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
                respectSpaceRatioButton.y = from;
                loopButton.y = from;
                CHelper.layout(
                    0, width,
                    CHelper.LayoutElement.blankSpace(20),
                    CHelper.LayoutElement.layoutX(respectSpaceRatioButton, 1),
                    CHelper.LayoutElement.blankSpace(10),
                    CHelper.LayoutElement.layoutX(loopButton, 1),
                    CHelper.LayoutElement.blankSpace(20)
                );
            }),
            CHelper.LayoutElement.blankSpace(10),
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
    
    @Override
    public void onClose() {
        // When `esc` is pressed return to the parent screen rather than setting screen to `null` which returns to the main menu.
        this.client.openScreen(this.parent);
    }
    
    private Consumer<DimTermWidget> getElementSelectCallback() {
        return w -> dimListWidget.setSelected(w);
    }
    
    @Override
    public void render(MatrixStack matrixStack, int mouseY, int i, float f) {
        this.renderBackground(matrixStack);
        
        
        if (isEnabled) {
            dimListWidget.render(matrixStack, mouseY, i, f);
        }
        
        super.render(matrixStack, mouseY, i, f);
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        textRenderer.drawWithShadow(
            matrixStack, this.title,
            20, 20, -1
        );
        
    }
    
    private void setEnabled(boolean cond) {
        isEnabled = cond;
        if (isEnabled) {
            toggleButton.setMessage(new TranslatableText("imm_ptl.altius_toggle_true"));
        }
        else {
            toggleButton.setMessage(new TranslatableText("imm_ptl.altius_toggle_false"));
        }
        addDimensionButton.visible = isEnabled;
        removeDimensionButton.visible = isEnabled;
        
        loopButton.visible = isEnabled;
        respectSpaceRatioButton.visible = isEnabled;
    }
    
    private void onAddDimension() {
        DimTermWidget selected = dimListWidget.getSelected();
        
        int position;
        if (selected == null) {
            position = 0;
        }
        else {
            position = dimListWidget.terms.indexOf(selected);
        }
        
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
                }, generatorOptionsSupplier1
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
        RegistryKey<World> inserted = dimListWidget.terms.get(insertedIndex).dimension;
        for (int i = dimListWidget.terms.size() - 1; i >= 0; i--) {
            if (dimListWidget.terms.get(i).dimension == inserted) {
                if (i != insertedIndex) {
                    dimListWidget.terms.remove(i);
                }
            }
        }
    }
    
}
