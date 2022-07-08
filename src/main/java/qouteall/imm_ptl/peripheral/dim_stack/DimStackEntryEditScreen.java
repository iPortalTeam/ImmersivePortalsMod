package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import qouteall.q_misc_util.my_util.GuiHelper;

public class DimStackEntryEditScreen extends Screen {
    
    
    private final DimStackScreen parent;
    private final DimEntryWidget editing;
    
    private final EditBox scaleField;
    private final Button flipButton;
    private final EditBox horizontalRotationField;
    private final EditBox topYField;
    private final EditBox bottomYField;
    private final EditBox bedrockBlockField;
    
    private final Button finishButton;
    
    private final GuiHelper.Rect scaleLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect flipLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect horizontalRotationLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect topYLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect bottomYLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect bedrockLabelRect = new GuiHelper.Rect();
    
    private final Button helpButton;
    
    protected DimStackEntryEditScreen(DimStackScreen parent, DimEntryWidget editing) {
        super(new TranslatableComponent("imm_ptl.dim_stack_edit_screen"));
        
        this.parent = parent;
        this.editing = editing;
        
        scaleField = new EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 20, Component.literal("you cannot see me")
        );
        scaleField.setValue(Double.toString(editing.entry.scale));
        scaleField.setHighlightPos(0);//without this the text won't render. mc gui is bugged
        scaleField.setCursorPosition(0);
        
        flipButton = new Button(
            0, 0, 0, 20,
            Component.translatable(editing.entry.flipped ? "imm_ptl.enabled" : "imm_ptl.disabled"),
            button -> {
                editing.entry.flipped = !editing.entry.flipped;
                button.setMessage(
                    Component.translatable(editing.entry.flipped ? "imm_ptl.enabled" : "imm_ptl.disabled")
                );
            }
        );
        
        horizontalRotationField = new EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 20,
            Component.literal("you cannot see me")
        );
        horizontalRotationField.setValue(Double.toString(editing.entry.horizontalRotation));
        horizontalRotationField.setCursorPosition(0);
        horizontalRotationField.setHighlightPos(0);
        
        topYField = new EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 20,
            Component.literal("you cannot see me")
        );
        if (editing.entry.topY != null) {
            topYField.setValue(Integer.toString(editing.entry.topY));
        }
        topYField.setCursorPosition(0);
        topYField.setHighlightPos(0);
        
        bottomYField = new EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 20,
            Component.literal("you cannot see me")
        );
        if (editing.entry.bottomY != null) {
            bottomYField.setValue(Integer.toString(editing.entry.bottomY));
        }
        bottomYField.setCursorPosition(0);
        bottomYField.setHighlightPos(0);
        
        bedrockBlockField = new EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 20,
            Component.literal("you cannot see me")
        );
        bedrockBlockField.setMaxLength(200);
        if (editing.entry.bedrockReplacementStr != null) {
            bedrockBlockField.setValue(editing.entry.bedrockReplacementStr);
        }
        bedrockBlockField.setCursorPosition(0);
        bedrockBlockField.setHighlightPos(0);
        
        finishButton = new Button(
            0, 0, 0, 20, Component.translatable("imm_ptl.finish"),
            button -> {
                try {
                    editing.entry.horizontalRotation = Double.parseDouble(horizontalRotationField.getValue());
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.horizontalRotation = 0;
                }
                
                try {
                    editing.entry.scale = Double.parseDouble(scaleField.getValue());
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.scale = 1;
                }
                
                try {
                    if (!topYField.getValue().isEmpty()) {
                        editing.entry.topY = Integer.parseInt(topYField.getValue());
                    }
                    else {
                        editing.entry.topY = null;
                    }
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.topY = null;
                }
                
                try {
                    if (!bottomYField.getValue().isEmpty()) {
                        editing.entry.bottomY = Integer.parseInt(bottomYField.getValue());
                    }
                    else {
                        editing.entry.bottomY = null;
                    }
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.bottomY = null;
                }
                
                editing.entry.bedrockReplacementStr = bedrockBlockField.getValue();
                
                Minecraft.getInstance().setScreen(parent);
            }
        );
        
        this.helpButton = DimStackScreen.createHelpButton(this);
    }
    
    @Override
    public void tick() {
        super.tick();
        scaleField.tick();
        horizontalRotationField.tick();
    }
    
    @Override
    protected void init() {
        addWidget(scaleField);
        addRenderableWidget(flipButton);
        addWidget(horizontalRotationField);
        addWidget(topYField);
        addWidget(bottomYField);
        addWidget(bedrockBlockField);
        addRenderableWidget(finishButton);
        addRenderableWidget(helpButton);
        
        GuiHelper.layout(
            0, height,
            GuiHelper.blankSpace(20),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(scaleLabelRect),
                    GuiHelper.layoutButtonVertically(scaleField)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(flipLabelRect),
                    GuiHelper.layoutButtonVertically(flipButton)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(horizontalRotationLabelRect),
                    GuiHelper.layoutButtonVertically(horizontalRotationField)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(topYLabelRect),
                    GuiHelper.layoutButtonVertically(topYField)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(bottomYLabelRect),
                    GuiHelper.layoutButtonVertically(bottomYField)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(bedrockLabelRect),
                    GuiHelper.layoutButtonVertically(bedrockBlockField)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20, GuiHelper.layoutButtonVertically(finishButton)),
            GuiHelper.blankSpace(20)
        );
        
        GuiHelper.layout(
            0, width,
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(150,
                GuiHelper.combine(
                    GuiHelper.layoutRectHorizontally(scaleLabelRect),
                    GuiHelper.layoutRectHorizontally(flipLabelRect),
                    GuiHelper.layoutRectHorizontally(horizontalRotationLabelRect),
                    GuiHelper.layoutRectHorizontally(topYLabelRect),
                    GuiHelper.layoutRectHorizontally(bottomYLabelRect),
                    GuiHelper.layoutRectHorizontally(bedrockLabelRect)
                )
            ),
            GuiHelper.blankSpace(20),
            GuiHelper.fixedLength(100,
                GuiHelper.combine(
                    GuiHelper.layoutButtonHorizontally(scaleField),
                    GuiHelper.layoutButtonHorizontally(flipButton),
                    GuiHelper.layoutButtonHorizontally(horizontalRotationField),
                    GuiHelper.layoutButtonHorizontally(topYField),
                    GuiHelper.layoutButtonHorizontally(bottomYField),
                    GuiHelper.layoutButtonHorizontally(bedrockBlockField)
                )
            ),
            GuiHelper.elasticBlankSpace()
        );
        
        GuiHelper.layout(
            0, width,
            GuiHelper.blankSpace(20),
            new GuiHelper.LayoutElement(
                true, 100,
                GuiHelper.layoutButtonHorizontally(finishButton)
            ),
            GuiHelper.elasticBlankSpace()
        );
        
        helpButton.x = width - 50;
        helpButton.y = 5;
    }
    
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
    
    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        
        super.render(matrices, mouseX, mouseY, delta);
        
        scaleField.render(matrices, mouseX, mouseY, delta);
        horizontalRotationField.render(matrices, mouseX, mouseY, delta);
        topYField.render(matrices, mouseX, mouseY, delta);
        bottomYField.render(matrices, mouseX, mouseY, delta);
        bedrockBlockField.render(matrices, mouseX, mouseY, delta);
        
        scaleLabelRect.renderTextLeft(Component.translatable("imm_ptl.scale"), matrices);
        flipLabelRect.renderTextLeft(Component.translatable("imm_ptl.flipped"), matrices);
        horizontalRotationLabelRect.renderTextLeft(Component.translatable("imm_ptl.horizontal_rotation"), matrices);
        topYLabelRect.renderTextLeft(Component.translatable("imm_ptl.top_y"), matrices);
        bottomYLabelRect.renderTextLeft(Component.translatable("imm_ptl.bottom_y"), matrices);
        bedrockLabelRect.renderTextLeft(Component.translatable("imm_ptl.bedrock_replacement"), matrices);
    }
}
