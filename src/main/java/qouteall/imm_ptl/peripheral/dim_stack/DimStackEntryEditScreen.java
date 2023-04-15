package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.GuiHelper;

import java.util.OptionalInt;

public class DimStackEntryEditScreen extends Screen {
    
    
    private final DimStackScreen parent;
    private final DimEntryWidget editing;
    
    private final EditBox scaleField;
    private final Button flipButton;
    private final EditBox horizontalRotationField;
    private final EditBox topYField;
    private final EditBox bottomYField;
    private final EditBox bedrockBlockField;
    private final Button connectsPreviousButton;
    private final Button connectsNextButton;
    
    private final Button finishButton;
    
    private final GuiHelper.Rect scaleLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect flipLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect horizontalRotationLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect topYLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect bottomYLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect bedrockLabelRect = new GuiHelper.Rect();
    private final GuiHelper.Rect connectsPreviousRect = new GuiHelper.Rect();
    private final GuiHelper.Rect connectsNextRect = new GuiHelper.Rect();
    
    private final Button helpButton;
    
    protected DimStackEntryEditScreen(
        DimStackScreen parent,
        DimEntryWidget editing,
        Runnable callback
    ) {
        super(Component.translatable("imm_ptl.dim_stack_edit_screen"));
        
        this.parent = parent;
        this.editing = editing;
        
        scaleField = new EditBox(
            Minecraft.getInstance().font,
            0, 0, 0, 20, Component.literal("you cannot see me")
        );
        scaleField.setValue(Double.toString(editing.entry.scale));
        scaleField.setHighlightPos(0);//without this the text won't render. mc gui is bugged
        scaleField.setCursorPosition(0);
        
        flipButton =
            Button.builder(
                    Component.translatable(editing.entry.flipped ? "imm_ptl.enabled" : "imm_ptl.disabled"),
                    button -> {
                        editing.entry.flipped = !editing.entry.flipped;
                        button.setMessage(
                            Component.translatable(editing.entry.flipped ? "imm_ptl.enabled" : "imm_ptl.disabled")
                        );
                    }
                )
                .build();
        
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
        
        connectsPreviousButton = Button.builder(
            Component.translatable(editing.entry.connectsPrevious ? "imm_ptl.enabled" : "imm_ptl.disabled"),
            button -> {
                editing.entry.connectsPrevious = !editing.entry.connectsPrevious;
                button.setMessage(
                    Component.translatable(editing.entry.connectsPrevious ? "imm_ptl.enabled" : "imm_ptl.disabled")
                );
            }
        ).build();
        
        connectsNextButton = Button.builder(
            Component.translatable(editing.entry.connectsNext ? "imm_ptl.enabled" : "imm_ptl.disabled"),
            button -> {
                editing.entry.connectsNext = !editing.entry.connectsNext;
                button.setMessage(
                    Component.translatable(editing.entry.connectsNext ? "imm_ptl.enabled" : "imm_ptl.disabled")
                );
            }
        ).build();
        
        finishButton = Button.builder(
            Component.translatable("imm_ptl.finish"),
            button -> {
                editing.entry.horizontalRotation =
                    Helper.parseDouble(horizontalRotationField.getValue()).orElse(0);
                
                editing.entry.scale =
                    Helper.parseDouble(scaleField.getValue()).orElse(1);
                
                OptionalInt topY = Helper.parseInt(topYField.getValue());
                editing.entry.topY = topY.isPresent() ? topY.getAsInt() : null;
                
                OptionalInt bottomY = Helper.parseInt(bottomYField.getValue());
                editing.entry.bottomY = bottomY.isPresent() ? bottomY.getAsInt() : null;
                
                editing.entry.bedrockReplacementStr = bedrockBlockField.getValue();
                
                Minecraft.getInstance().setScreen(parent);
                callback.run();
            }
        ).build();
        
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
        addWidget(connectsPreviousButton);
        addWidget(connectsNextButton);
        addRenderableWidget(finishButton);
        addRenderableWidget(helpButton);
        
        GuiHelper.layout(
            0, height,
            GuiHelper.blankSpace(5),
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
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(connectsPreviousRect),
                    GuiHelper.layoutButtonVertically(connectsPreviousButton)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20,
                GuiHelper.combine(
                    GuiHelper.layoutRectVertically(connectsNextRect),
                    GuiHelper.layoutButtonVertically(connectsNextButton)
                )
            ),
            GuiHelper.elasticBlankSpace(),
            GuiHelper.fixedLength(20, GuiHelper.layoutButtonVertically(finishButton)),
            GuiHelper.blankSpace(5)
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
                    GuiHelper.layoutRectHorizontally(bedrockLabelRect),
                    GuiHelper.layoutRectHorizontally(connectsPreviousRect),
                    GuiHelper.layoutRectHorizontally(connectsNextRect)
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
                    GuiHelper.layoutButtonHorizontally(bedrockBlockField),
                    GuiHelper.layoutButtonHorizontally(connectsPreviousButton),
                    GuiHelper.layoutButtonHorizontally(connectsNextButton)
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
        
        helpButton.setX(width - 50);
        helpButton.setY(5);
        helpButton.setWidth(20);
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
        connectsPreviousButton.render(matrices, mouseX, mouseY, delta);
        connectsNextButton.render(matrices, mouseX, mouseY, delta);
        
        scaleLabelRect.renderTextLeft(Component.translatable("imm_ptl.scale"), matrices);
        flipLabelRect.renderTextLeft(Component.translatable("imm_ptl.flipped"), matrices);
        horizontalRotationLabelRect.renderTextLeft(Component.translatable("imm_ptl.horizontal_rotation"), matrices);
        topYLabelRect.renderTextLeft(Component.translatable("imm_ptl.top_y"), matrices);
        bottomYLabelRect.renderTextLeft(Component.translatable("imm_ptl.bottom_y"), matrices);
        bedrockLabelRect.renderTextLeft(Component.translatable("imm_ptl.bedrock_replacement"), matrices);
        connectsPreviousRect.renderTextLeft(Component.translatable("imm_ptl.connects_previous"), matrices);
        connectsNextRect.renderTextLeft(Component.translatable("imm_ptl.connects_next"), matrices);
    }
}
