package com.qouteall.imm_ptl_peripheral.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

public class AltiusEditScreen extends Screen {
    
    
    private final AltiusScreen parent;
    private final DimEntryWidget editing;
    
    protected AltiusEditScreen(AltiusScreen parent, DimEntryWidget editing) {
        super(new TranslatableText("imm_ptl.dim_stack_edit_screen"));
        
        this.parent = parent;
        this.editing = editing;
    }
    
    @Override
    public void onClose() {
        MinecraftClient.getInstance().openScreen(parent);
    }
}
