package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.CHelper;
import qouteall.q_misc_util.my_util.GuiHelper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class DimStackScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(DimStackScreen.class);
    
    private final DimStackGuiController controller;
    
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
    
    public final DimListWidget dimListWidget;
    
    private boolean isEnabled = false;
    
    public DimStackScreen(
        @Nullable Screen parent,
        DimStackGuiController controller
    ) {
        super(Component.translatable("imm_ptl.altius_screen"));
        this.parent = parent;
        
        toggleButton = Button.builder(
            Component.literal("..."),
            (buttonWidget) -> {
                controller.toggleEnabled();
            }
        ).build();
        
        loopButton = Button.builder(
            Component.literal("..."),
            (buttonWidget) -> {
                controller.toggleLoop();
            }
        ).build();
        
        gravityModeButton = Button.builder(
            Component.literal("..."),
            (gravityModeButton) -> {
                controller.toggleGravityMode();
            }
        ).build();
        
        finishButton = Button.builder(
            Component.translatable("imm_ptl.finish"),
            (buttonWidget) -> {
                controller.onFinish();
            }
        ).build();
        this.controller = controller;
        addDimensionButton = Button.builder(
            Component.translatable("imm_ptl.dim_stack_add"),
            (buttonWidget) -> {
                onAddEntry();
            }
        ).build();
        removeDimensionButton = Button.builder(
            Component.translatable("imm_ptl.dim_stack_remove"),
            (buttonWidget) -> {
                onRemoveEntry();
            }
        ).build();
        
        editButton = Button.builder(
            Component.translatable("imm_ptl.dim_stack_edit"),
            (buttonWidget) -> {
                onEditEntry();
            }
        ).build();
        
        dimListWidget = new DimListWidget(
            width,
            height,
            100,
            200,
            DimEntryWidget.widgetHeight,
            this,
            DimListWidget.Type.mainDimensionList,
            controller::onDragged
        );
        
        helpButton = createHelpButton(this);
        
        this.setAsPresetButton = Button.builder(
            Component.translatable("imm_ptl.set_as_dim_stack_default"),
            button -> {
                this.controller.setAsDefault();
            }
        ).build();
    }
    
    public void setLoopEnabled(boolean loopEnabled) {
        loopButton.setMessage(Component.translatable(
            loopEnabled ? "imm_ptl.loop_enabled" : "imm_ptl.loop_disabled"
        ));
    }
    
    public void setGravityTransformEnabled(boolean gravityTransformEnabled) {
        gravityModeButton.setMessage(Component.translatable(
            gravityTransformEnabled ? "imm_ptl.dim_stack.gravity_transform_enabled" :
                "imm_ptl.dim_stack.gravity_transform_disabled"
        ));
    }
    
    public static Button createHelpButton(Screen parent) {
        return Button.builder(
            Component.literal("?"),
            button -> {
                CHelper.openLinkConfirmScreen(
                    parent, "https://qouteall.fun/immptl/wiki/Dimension-Stack"
                );
            }
        ).build();
    }
    
    public DimEntryWidget createDimEntryWidget(DimStackEntry entry) {
        return new DimEntryWidget(
            entry.getDimension(),
            dimListWidget,
            getElementSelectCallback(),
            entry
        );
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
        
        addWidget(dimListWidget);
        
        GuiHelper.layout(
            0, height,
            GuiHelper.blankSpace(5),
            new GuiHelper.LayoutElement(true, 20, (from, to) -> {
                helpButton.setX(width - 30);
                helpButton.setY(from);
                helpButton.setWidth(20);
                
                setAsPresetButton.setX(width - 125);
                setAsPresetButton.setY(from);
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
                finishButton.setY(from);
                addDimensionButton.setY(from);
                removeDimensionButton.setY(from);
                editButton.setY(from);
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
        // When `esc` is pressed, it's the same as pressing "Finish".
        // Don't return to the main menu.
        controller.onFinish();
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
    
    public void setEnabled(boolean cond) {
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
            position = dimListWidget.children().indexOf(selected);
        }
        
        if (position < 0 || position > dimListWidget.children().size()) {
            position = -1;
        }
        
        int insertingPosition = position + 1;
        
        Minecraft.getInstance().setScreen(
            new SelectDimensionScreen(
                this,
                dimensionType -> {
                    controller.addEntry(insertingPosition, new DimStackEntry(dimensionType));
                },
                controller.getDimensionList()
            )
        );
    }
    
    private void onRemoveEntry() {
        DimEntryWidget selected = dimListWidget.getSelected();
        if (selected == null) {
            return;
        }
        
        int position = dimListWidget.children().indexOf(selected);
        
        if (position == -1) {
            return;
        }
        
        controller.removeEntry(position);
    }
    
    private void onEditEntry() {
        DimEntryWidget selected = dimListWidget.getSelected();
        if (selected == null) {
            return;
        }
        
        Minecraft.getInstance().setScreen(new DimStackEntryEditScreen(
            this, selected,
            () -> {
                int newlyChangingEntryIndex = dimListWidget.children().indexOf(selected);
                if (newlyChangingEntryIndex == -1) {
                    LOGGER.error("The edited entry is missing in the list");
                    return;
                }
                controller.editEntry(newlyChangingEntryIndex, selected.entry);
            }
        ));
    }
    
}
