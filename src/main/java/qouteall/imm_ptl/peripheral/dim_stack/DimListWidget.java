package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import qouteall.q_misc_util.Helper;

import org.jetbrains.annotations.Nullable;

public class DimListWidget extends AbstractSelectionList<DimEntryWidget> {
    
    public static final int ROW_WIDTH = 300;
    
    public static interface DraggingCallback {
        void run(int selectedIndex, int mouseOnIndex);
    }
    
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
        setRenderBackground(false);
        setRenderTopAndBottom(false);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (type == Type.mainDimensionList && draggingCallback != null) {
            DimEntryWidget selected = getSelected();
        
            if (selected != null) {
                DimEntryWidget mouseOn = getEntryAtPosition(mouseX, mouseY);
                if (mouseOn != null) {
                    if (mouseOn != selected) {
                        int selectedIndex = children().indexOf(selected);
                        int mouseOnIndex = children().indexOf(mouseOn);
                        if (selectedIndex != -1 && mouseOnIndex != -1) {
                            draggingCallback.run(selectedIndex, mouseOnIndex);
                        }
                        else {
                            Helper.err("Invalid dragging");
                        }
                    }
                }
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    // make it wider
    @Override
    public int getRowWidth() {
        return ROW_WIDTH;
    }
    
    @Override
    protected int getScrollbarPosition() {
        return (width - ROW_WIDTH) / 2 + ROW_WIDTH;
    }
}
