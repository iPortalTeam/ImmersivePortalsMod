package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import qouteall.q_misc_util.my_util.GuiHelper;

public class AltiusEditScreen extends Screen {
    
    
    private final AltiusScreen parent;
    private final DimEntryWidget editing;
    
    private final TextFieldWidget scaleField;
    private final ButtonWidget flipButton;
    private final TextFieldWidget horizontalRotationField;
    private final TextFieldWidget topYField;
    private final TextFieldWidget bottomYField;
    private final TextFieldWidget bedrockBlockField;
    
    private final ButtonWidget finishButton;
    
    private final GuiHelper.Rect scaleLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect flipLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect horizontalRotationLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect topYLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect bottomYLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect bedrockLabelRect = new GuiHelper.Rect();
    
    private final ButtonWidget helpButton;
    
    protected AltiusEditScreen(AltiusScreen parent, DimEntryWidget editing) {
        super(new TranslatableText("imm_ptl.dim_stack_edit_screen"));
        
        this.parent = parent;
        this.editing = editing;
        
        scaleField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0, 0, 0, 20, new LiteralText("heh")
        );
        scaleField.setText(Double.toString(editing.entry.scale));
        scaleField.setSelectionEnd(0);//without this the text won't render. mc gui is bugged
        scaleField.setSelectionStart(0);
        
        flipButton = new ButtonWidget(
            0, 0, 0, 20,
            new TranslatableText(editing.entry.flipped ? "imm_ptl.enabled" : "imm_ptl.disabled"),
            button -> {
                editing.entry.flipped = !editing.entry.flipped;
                button.setMessage(
                    new TranslatableText(editing.entry.flipped ? "imm_ptl.enabled" : "imm_ptl.disabled")
                );
            }
        );
        
        horizontalRotationField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0, 0, 0, 20,
            new LiteralText("you cannot see me")
        );
        horizontalRotationField.setText(Double.toString(editing.entry.horizontalRotation));
        horizontalRotationField.setSelectionStart(0);
        horizontalRotationField.setSelectionEnd(0);
        
        topYField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0, 0, 0, 20,
            new LiteralText("you cannot see me")
        );
        if (editing.entry.topY != null) {
            topYField.setText(Integer.toString(editing.entry.topY));
        }
        topYField.setSelectionStart(0);
        topYField.setSelectionEnd(0);
        
        bottomYField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0, 0, 0, 20,
            new LiteralText("you cannot see me")
        );
        if (editing.entry.bottomY != null) {
            bottomYField.setText(Integer.toString(editing.entry.bottomY));
        }
        bottomYField.setSelectionStart(0);
        bottomYField.setSelectionEnd(0);
        
        bedrockBlockField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0, 0, 0, 20,
            new LiteralText("you cannot see me")
        );
        if (editing.entry.bedrockReplacementStr != null) {
            bedrockBlockField.setText(editing.entry.bedrockReplacementStr);
        }
        bedrockBlockField.setSelectionStart(0);
        bedrockBlockField.setSelectionEnd(0);
        
        finishButton = new ButtonWidget(
            0, 0, 0, 20, new TranslatableText("imm_ptl.finish"),
            button -> {
                try {
                    editing.entry.horizontalRotation = Double.parseDouble(horizontalRotationField.getText());
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.horizontalRotation = 0;
                }
                
                try {
                    editing.entry.scale = Double.parseDouble(scaleField.getText());
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.scale = 1;
                }
                
                try {
                    if (!topYField.getText().isEmpty()) {
                        editing.entry.topY = Integer.parseInt(topYField.getText());
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
                    if (!bottomYField.getText().isEmpty()) {
                        editing.entry.bottomY = Integer.parseInt(bottomYField.getText());
                    }
                    else {
                        editing.entry.bottomY = null;
                    }
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    editing.entry.bottomY = null;
                }
                
                editing.entry.bedrockReplacementStr = bedrockBlockField.getText();
                
                MinecraftClient.getInstance().setScreen(parent);
            }
        );
        
        this.helpButton = AltiusScreen.createHelpButton(this);
    }
    
    @Override
    public void tick() {
        super.tick();
        scaleField.tick();
        horizontalRotationField.tick();
    }
    
    @Override
    protected void init() {
        addSelectableChild(scaleField);
        addDrawableChild(flipButton);
        addSelectableChild(horizontalRotationField);
        addSelectableChild(topYField);
        addSelectableChild(bottomYField);
        addSelectableChild(bedrockBlockField);
        addDrawableChild(finishButton);
        addDrawableChild(helpButton);
        
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
        MinecraftClient.getInstance().setScreen(parent);
    }
    
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        
        super.render(matrices, mouseX, mouseY, delta);
        
        scaleField.render(matrices, mouseX, mouseY, delta);
        horizontalRotationField.render(matrices, mouseX, mouseY, delta);
        topYField.render(matrices, mouseX, mouseY, delta);
        bottomYField.render(matrices, mouseX, mouseY, delta);
        bedrockBlockField.render(matrices, mouseX, mouseY, delta);
        
        scaleLabelRect.renderTextLeft(new TranslatableText("imm_ptl.scale"), matrices);
        flipLabelRect.renderTextLeft(new TranslatableText("imm_ptl.flipped"), matrices);
        horizontalRotationLabelRect.renderTextLeft(new TranslatableText("imm_ptl.horizontal_rotation"), matrices);
        topYLabelRect.renderTextLeft(new TranslatableText("imm_ptl.top_y"), matrices);
        bottomYLabelRect.renderTextLeft(new TranslatableText("imm_ptl.bottom_y"), matrices);
        bedrockLabelRect.renderTextLeft(new TranslatableText("imm_ptl.bedrock_replacement"), matrices);
    }
}
