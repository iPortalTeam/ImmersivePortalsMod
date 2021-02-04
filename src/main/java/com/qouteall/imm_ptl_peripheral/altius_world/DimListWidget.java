package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.GuiHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.List;

public class DimListWidget extends EntryListWidget<DimEntryWidget> {
    public final List<DimEntryWidget> entryWidgets = new ArrayList<>();
    public final Screen parent;
    private final Type type;
    
    private LoopSwitchEntry loopSwitchEntry;
    
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
    
        if (type == Type.mainDimensionList) {
            loopSwitchEntry = new LoopSwitchEntry(this);
        }
    }
    
    public void update() {
        this.clearEntries();
        this.entryWidgets.forEach(this::addEntry);
    
        if (type == Type.mainDimensionList) {
//            addEntry(loopSwitchEntry);
        }
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
    
    public static class LoopSwitchEntry extends ElementListWidget.Entry<LoopSwitchEntry> {
        
        public final DimListWidget parent;
        
        private final ArrayList<Element> children = new ArrayList<>();
        
        private final ButtonWidget loopSwitchButton;
        
        public LoopSwitchEntry(DimListWidget parent) {
            this.parent = parent;
            
            this.loopSwitchButton = new ButtonWidget(
                0, 0, 100, 20,
                new TranslatableText(getParentParent().loopEnabled ?
                    "imm_ptl.enabled" : "imm_ptl.disabled"),
                button -> {
                    getParentParent().loopEnabled = !getParentParent().loopEnabled;
                    button.setMessage(
                        new TranslatableText(getParentParent().loopEnabled ?
                            "imm_ptl.enabled" : "imm_ptl.disabled")
                    );
                }
            );
        }
        
        public AltiusScreen getParentParent() {
            return ((AltiusScreen) parent.parent);
        }
        
        @Override
        public void render(
            MatrixStack matrixStack,
            int index,
            int y,
            int x,
            int rowWidth,
            int itemHeight,
            int mouseX,
            int mouseY,
            boolean bl,
            float delta
        ) {
            new GuiHelper.Rect(x, y, x + 100, y + itemHeight)
                .renderTextLeft(new TranslatableText("imm_ptl.loop"), matrixStack);
            
            loopSwitchButton.render(matrixStack, mouseX, mouseY, delta);
        }
        
        @Override
        public List<? extends Element> children() {
            return children;
        }
    }
}
