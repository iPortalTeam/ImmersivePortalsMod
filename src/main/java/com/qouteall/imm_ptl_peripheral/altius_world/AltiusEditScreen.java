package com.qouteall.imm_ptl_peripheral.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.List;

public class AltiusEditScreen extends Screen {
    
    
    private final AltiusScreen parent;
    private final DimEntryWidget editing;
    
    private final TextFieldWidget scaleField;
    private final ButtonWidget flipButton;
    private final TextFieldWidget horizontalRotationField;
    private final ButtonWidget loopButton;
    
    protected AltiusEditScreen(AltiusScreen parent, DimEntryWidget editing) {
        super(new TranslatableText("imm_ptl.dim_stack_edit_screen"));
        
        this.parent = parent;
        this.editing = editing;
        
        scaleField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            100, 0, 100, 20, new LiteralText("heh")
        );
        
        flipButton = new ButtonWidget(
            100, 100, 100, 20,
            new TranslatableText(editing.entry.flipped ? "imm_ptl.flip_enabled" : "imm_ptl.flip_disabled"),
            button -> {
            
            }
        );
        
        horizontalRotationField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0, 100, 100, 20,
            new LiteralText("you cannot see me")
        );
        
        loopButton = new ButtonWidget(
            0, 0, 100, 20,
            new TranslatableText(
                parent.loopEnabled ? "imm_ptl.loop_enabled" : "imm_ptl.loop_disabled"
            ),
            button -> {
                parent.loopEnabled = !parent.loopEnabled;
                button.setMessage(new TranslatableText(
                    parent.loopEnabled ? "imm_ptl.loop_enabled" : "imm_ptl.loop_disabled"
                ));
            }
        );
        
        List<DimEntryWidget> entryWidgets = parent.dimListWidget.entryWidgets;
        int editingIndex = entryWidgets.indexOf(editing);
        if (editingIndex == entryWidgets.size() - 1 || editingIndex == 0) {
            loopButton.visible = true;
        }
        else {
            loopButton.visible = false;
        }
        
    }
    
    @Override
    protected void init() {
        addChild(scaleField);
        
        addButton(flipButton);
        
        addChild(horizontalRotationField);
        
        addButton(loopButton);
    }
    
    @Override
    public void onClose() {
        MinecraftClient.getInstance().openScreen(parent);
    }
    
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        
        super.render(matrices, mouseX, mouseY, delta);
        
        scaleField.render(matrices, mouseX, mouseY, delta);
        horizontalRotationField.render(matrices, mouseX, mouseY, delta);
    }
}
