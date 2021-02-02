package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EntryListWidget;

import java.util.ArrayList;
import java.util.List;

public class DimListWidget extends EntryListWidget<DimTermWidget> {
    public final List<DimTermWidget> terms = new ArrayList<>();
    public final Screen parent;
    private final Type type;
    
    public static enum Type {
        mainDimensionList, addDimensionList
    }
    
    public DimListWidget(
        int width,
        int height,
        int top,
        int bottom,
        int itemHeight,
        Screen parent,
        Type type
    ) {
        super(MinecraftClient.getInstance(), width, height, top, bottom, itemHeight);
        this.parent = parent;
        this.type = type;
    }
    
    public void update() {
        this.clearEntries();
        this.terms.forEach(this::addEntry);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (type == Type.mainDimensionList) {
            DimTermWidget selected = getSelected();
            
            if (selected != null) {
                DimTermWidget mouseOn = getEntryAtPosition(mouseX, mouseY);
                if (mouseOn != null) {
                    if (mouseOn != selected) {
                        switchEntries(selected, mouseOn);
                    }
                }
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    private void switchEntries(DimTermWidget a, DimTermWidget b) {
        int i1 = terms.indexOf(a);
        int i2 = terms.indexOf(b);
        if (i1 == -1 || i2 == -1) {
            Helper.err("Dimension Stack GUI Abnormal");
            return;
        }
        
        DimTermWidget temp = terms.get(i1);
        terms.set(i1, terms.get(i2));
        terms.set(i2, temp);
        
        update();
    }
}
