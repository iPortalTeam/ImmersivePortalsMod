package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EntryListWidget;

import java.util.ArrayList;
import java.util.List;

public class DimListWidget extends EntryListWidget<DimEntryWidget> {
    public final List<DimEntryWidget> entryWidgets = new ArrayList<>();
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
        this.entryWidgets.forEach(this::addEntry);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (type == Type.mainDimensionList) {
            DimEntryWidget selected = getSelected();
            
            if (selected != null) {
                DimEntryWidget mouseOn = getEntryAtPosition(mouseX, mouseY);
                if (mouseOn != null) {
                    if (mouseOn != selected) {
                        switchEntries(selected, mouseOn);
                    }
                }
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    private void switchEntries(DimEntryWidget a, DimEntryWidget b) {
        int i1 = entryWidgets.indexOf(a);
        int i2 = entryWidgets.indexOf(b);
        if (i1 == -1 || i2 == -1) {
            Helper.err("Dimension Stack GUI Abnormal");
            return;
        }
        
        DimEntryWidget temp = entryWidgets.get(i1);
        entryWidgets.set(i1, entryWidgets.get(i2));
        entryWidgets.set(i2, temp);
        
        update();
    }
}
