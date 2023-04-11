package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DimListWidget extends AbstractSelectionList<DimEntryWidget> {
    public static interface DraggingCallback{
        // pass this because java doesn't allow using field in initialization lambda
        void run(DimListWidget self, DimEntryWidget selected, DimEntryWidget mouseOn);
    }
    
    public final List<DimEntryWidget> entryWidgets = new ArrayList<>();
    public final Screen parent;
    private final Type type;
    @Nullable
    private final DraggingCallback draggingCallback;
    
    @Override
    public void updateNarration(NarrationElementOutput builder) {
    
    }
    
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
        Type type,
        @Nullable DraggingCallback draggingCallback
    ) {
        super(Minecraft.getInstance(), width, height, top, bottom, itemHeight);
        this.parent = parent;
        this.type = type;
        this.draggingCallback = draggingCallback;
    }
    
    public void update() {
        this.clearEntries();
        List<DimEntryWidget> widgets = this.entryWidgets;
        for (int i = 0; i < widgets.size(); i++) {
            DimEntryWidget entryWidget = widgets.get(i);
            addEntry(entryWidget);
            entryWidget.isFirst = (i == 0);
            entryWidget.isLast = (i == widgets.size() - 1);
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
                        assert draggingCallback != null;
                        draggingCallback.run(this,selected, mouseOn);
                    }
                }
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    public void switchEntries(DimEntryWidget selected, DimEntryWidget mouseOn) {
        int i1 = entryWidgets.indexOf(selected);
        int i2 = entryWidgets.indexOf(mouseOn);
        if (i1 == -1 || i2 == -1) {
            Helper.err("Dimension Stack GUI Abnormal");
            return;
        }
        
        DimEntryWidget temp = entryWidgets.get(i1);
        entryWidgets.set(i1, entryWidgets.get(i2));
        entryWidgets.set(i2, temp);
        
        update();
        setSelected(selected);
    }
}
