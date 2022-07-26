package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.q_misc_util.my_util.GuiHelper;
import qouteall.q_misc_util.my_util.MyTaskList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class DimStackScreen extends Screen {
    @org.jetbrains.annotations.Nullable
    public final Screen parent;
    private final Button finishButton;
    private final Button toggleButton;
    private final Button addDimensionButton;
    private final Button removeDimensionButton;
    private final Button editButton;
    
    private final Button helpButton;
    private final Button setAsPresetButton;
    
    private final Button loopButton;
    private final Button gravityModeButton;
    
    private int titleY;
    
    public boolean isEnabled = false;
    public final DimListWidget dimListWidget;
    
    public boolean loopEnabled = false;
    public boolean gravityTransformEnabled = false;
    
    public final Function<Screen, List<ResourceKey<Level>>> dimensionListSupplier;
    private final Consumer<DimStackInfo> finishCallback;
    
    public DimStackScreen(
        @Nullable Screen parent,
        Function<Screen, List<ResourceKey<Level>>> dimensionListSupplier,
        Consumer<DimStackInfo> finishCallback
    ) {
        super(Component.translatable("imm_ptl.altius_screen"));
        this.parent = parent;
        this.dimensionListSupplier = dimensionListSupplier;
        this.finishCallback = finishCallback;
        
        toggleButton = new Button(
            0, 0, 150, 20,
            Component.literal("..."),
            (buttonWidget) -> {
                setEnabled(!isEnabled);
            }
        );
        
        loopButton = new Button(
            0, 0, 150, 20,
            Component.literal("..."),
            (buttonWidget) -> {
                loopEnabled = !loopEnabled;
                updateButtonText();
            }
        );
        
        gravityModeButton = new Button(
            0, 0, 150, 20,
            Component.literal("..."),
            (gravityModeButton) -> {
                gravityTransformEnabled = !gravityTransformEnabled;
                updateButtonText();
            }
        );
        
        finishButton = new Button(
            0, 0, 72, 20,
            Component.translatable("imm_ptl.finish"),
            (buttonWidget) -> {
                Minecraft.getInstance().setScreen(parent);
                finishCallback.accept(getDimStackInfo());
            }
        );
        addDimensionButton = new Button(
            0, 0, 72, 20,
            Component.translatable("imm_ptl.dim_stack_add"),
            (buttonWidget) -> {
                onAddEntry();
            }
        );
        removeDimensionButton = new Button(
            0, 0, 72, 20,
            Component.translatable("imm_ptl.dim_stack_remove"),
            (buttonWidget) -> {
                onRemoveEntry();
            }
        );
        
        editButton = new Button(
            0, 0, 72, 20,
            Component.translatable("imm_ptl.dim_stack_edit"),
            (buttonWidget) -> {
                onEditEntry();
            }
        );
        
        dimListWidget = new DimListWidget(
            width,
            height,
            100,
            200,
            DimEntryWidget.widgetHeight,
            this,
            DimListWidget.Type.mainDimensionList
        );
        
        helpButton = createHelpButton(this);
        
        this.setAsPresetButton = new Button(
            0, 0, 30, 20,
            Component.translatable("imm_ptl.set_as_dim_stack_default"),
            button -> {
                onSetAsDefault();
            }
        );
        
        loadDimensionStackPreset();
        
        updateButtonText();
    }
    
    private void loadDimensionStackPreset() {
        DimStackInfo preset = IPOuterClientMisc.getDimStackPreset();
        
        if (preset != null) {
            setEnabled(true);
            
            loopEnabled = preset.loop;
            gravityTransformEnabled = preset.gravityTransform;
            dimListWidget.entryWidgets.clear();
            for (DimStackEntry entry : preset.entries) {
                dimListWidget.entryWidgets.add(createDimEntryWidget(entry));
            }
        }
        else {
            setEnabled(false);
            
            if (IPGlobal.enableAlternateDimensions) {
                dimListWidget.entryWidgets.add(createDimEntryWidget(
                    new DimStackEntry(AlternateDimensions.alternate5)
                ));
                dimListWidget.entryWidgets.add(createDimEntryWidget(
                    new DimStackEntry(AlternateDimensions.alternate1)
                ));
            }
            dimListWidget.entryWidgets.add(createDimEntryWidget(new DimStackEntry(Level.OVERWORLD)));
            dimListWidget.entryWidgets.add(createDimEntryWidget(new DimStackEntry(Level.NETHER)));
        }
    }
    
    private void updateButtonText() {
        loopButton.setMessage(Component.translatable(
            loopEnabled ? "imm_ptl.loop_enabled" : "imm_ptl.loop_disabled"
        ));
        
        gravityModeButton.setMessage(Component.translatable(
            gravityTransformEnabled ? "imm_ptl.dim_stack.gravity_transform_enabled" :
                "imm_ptl.dim_stack.gravity_transform_disabled"
        ));
    }
    
    public static Button createHelpButton(Screen parent) {
        return new Button(
            0, 0, 30, 20,
            Component.literal("?"),
            button -> {
                CHelper.openLinkConfirmScreen(
                    parent, "https://qouteall.fun/immptl/wiki/Dimension-Stack"
                );
            }
        );
    }
    
    private DimEntryWidget createDimEntryWidget(DimStackEntry entry) {
        return new DimEntryWidget(
            entry.getDimension(),
            dimListWidget,
            getElementSelectCallback(),
            DimEntryWidget.Type.withAdvancedOptions,
            entry
        );
    }
    
    @Nullable
    public DimStackInfo getDimStackInfo() {
        if (isEnabled) {
            return new DimStackInfo(
                dimListWidget.entryWidgets.stream().map(
                    dimEntryWidget -> dimEntryWidget.entry
                ).collect(Collectors.toList()),
                loopEnabled,
                gravityTransformEnabled
            );
        }
        else {
            return null;
        }
    }
    
    @Override
    protected void init() {
        
        addRenderableWidget(toggleButton);
        addRenderableWidget(finishButton);
        addRenderableWidget(addDimensionButton);
        addRenderableWidget(removeDimensionButton);
        
        addRenderableWidget(editButton);
        addRenderableWidget(helpButton);
        addRenderableWidget(setAsPresetButton);
        addRenderableWidget(loopButton);
        addRenderableWidget(gravityModeButton);
        
        setEnabled(isEnabled);
        
        addWidget(dimListWidget);
        
        dimListWidget.update();
        
        GuiHelper.layout(
            0, height,
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(true, 20, (from, to) -> {
                helpButton.x = width - 30;
                helpButton.y = from;
                helpButton.setWidth(20);
                
                setAsPresetButton.x = width - 125;
                setAsPresetButton.y = from;
                setAsPresetButton.setWidth(90);
            }),
            new GuiHelper.LayoutElement(
                true, 20,
                GuiHelper.combine(
                    GuiHelper.layoutButtonVertically(toggleButton),
                    GuiHelper.layoutButtonVertically(loopButton),
                    GuiHelper.layoutButtonVertically(gravityModeButton)
                )
            ),
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(false, 1, (from, to) -> {
                dimListWidget.updateSize(
                    width, height,
                    from, to
                );
            }),
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(true, 20, (from, to) -> {
                finishButton.y = from;
                addDimensionButton.y = from;
                removeDimensionButton.y = from;
                editButton.y = from;
                GuiHelper.layout(
                    0, width,
                    GuiHelper.blankSpace(10),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(finishButton)
                    ),
                    GuiHelper.blankSpace(5),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(addDimensionButton)
                    ),
                    GuiHelper.blankSpace(5),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(removeDimensionButton)
                    ),
                    GuiHelper.blankSpace(5),
                    new GuiHelper.LayoutElement(
                        false, 1,
                        GuiHelper.layoutButtonHorizontally(editButton)
                    ),
                    GuiHelper.blankSpace(10)
                );
            }),
            GuiHelper.blankSpace(5)
        );
        
        GuiHelper.layout(
            0, width,
            GuiHelper.blankSpace(10),
            new GuiHelper.LayoutElement(
                false, 10, GuiHelper.layoutButtonHorizontally(toggleButton)
            ),
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(
                false, 8, GuiHelper.layoutButtonHorizontally(loopButton)
            ),
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(
                false, 10, GuiHelper.layoutButtonHorizontally(gravityModeButton)
            ),
            GuiHelper.blankSpace(10)
        );
    }
    
    @Override
    public void onClose() {
        // When `esc` is pressed return to the parent screen rather than setting screen to `null` which returns to the main menu.
        this.minecraft.setScreen(this.parent);
    }
    
    private Consumer<DimEntryWidget> getElementSelectCallback() {
        return w -> dimListWidget.setSelected(w);
    }
    
    @Override
    public void render(PoseStack matrixStack, int mouseY, int i, float f) {
        this.renderBackground(matrixStack);
        
        
        if (isEnabled) {
            dimListWidget.render(matrixStack, mouseY, i, f);
        }
        
        super.render(matrixStack, mouseY, i, f);
        
        Font textRenderer = Minecraft.getInstance().font;
        textRenderer.drawShadow(
            matrixStack, this.title,
            20, 10, -1
        );
        
    }
    
    private void setEnabled(boolean cond) {
        isEnabled = cond;
        if (isEnabled) {
            toggleButton.setMessage(Component.translatable("imm_ptl.altius_toggle_true"));
        }
        else {
            toggleButton.setMessage(Component.translatable("imm_ptl.altius_toggle_false"));
        }
        addDimensionButton.visible = isEnabled;
        removeDimensionButton.visible = isEnabled;
        editButton.visible = isEnabled;
        loopButton.visible = isEnabled;
        gravityModeButton.visible = isEnabled;
    }
    
    private void onAddEntry() {
        DimEntryWidget selected = dimListWidget.getSelected();
        
        int position;
        if (selected == null) {
            position = 0;
        }
        else {
            position = dimListWidget.entryWidgets.indexOf(selected);
        }
        
        if (position < 0 || position > dimListWidget.entryWidgets.size()) {
            position = -1;
        }
        
        int insertingPosition = position + 1;
        
        Minecraft.getInstance().setScreen(
            new SelectDimensionScreen(
                this,
                dimensionType -> {
                    dimListWidget.entryWidgets.add(
                        insertingPosition,
                        createDimEntryWidget(new DimStackEntry(dimensionType))
                    );
                    removeDuplicate(insertingPosition);
                    dimListWidget.update();
                }
            )
        );

//        IPGlobal.preTotalRenderTaskList.addTask(MyTaskList.withDelay(1, () -> {
//
//            return true;
//        }));
    }
    
    private void onRemoveEntry() {
        DimEntryWidget selected = dimListWidget.getSelected();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.entryWidgets.indexOf(selected);
        
        if (position == -1) {
            return;
        }
        
        dimListWidget.entryWidgets.remove(position);
        dimListWidget.update();
    }
    
    private void onEditEntry() {
        DimEntryWidget selected = dimListWidget.getSelected();
        if (selected == null) {
            return;
        }
        
        Minecraft.getInstance().setScreen(new DimStackEntryEditScreen(
            this, selected
        ));
    }
    
    private void removeDuplicate(int insertedIndex) {
        ResourceKey<Level> inserted = dimListWidget.entryWidgets.get(insertedIndex).dimension;
        for (int i = dimListWidget.entryWidgets.size() - 1; i >= 0; i--) {
            if (dimListWidget.entryWidgets.get(i).dimension == inserted) {
                if (i != insertedIndex) {
                    dimListWidget.entryWidgets.remove(i);
                }
            }
        }
    }
    
    private void onSetAsDefault() {
        IPOuterClientMisc.setDimStackPreset(getDimStackInfo());
        
        DimStackScreen currentScreen = this;
        
        Minecraft.getInstance().setScreen(new GenericDirtMessageScreen(
            Component.translatable("imm_ptl.dim_stack_default_updated")
        ));
        
        IPGlobal.preTotalRenderTaskList.addTask(MyTaskList.withTimeDelayedFromNow(
            1,
            MyTaskList.oneShotTask(() -> {
                Minecraft.getInstance().setScreen(currentScreen);
            })
        ));
    }
    
}
