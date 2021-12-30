package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.GuiHelper;

import java.util.ArrayList;
import java.util.List;

public class DimListWidget extends EntryListWidget<DimEntryWidget> {
    public final List<DimEntryWidget> entryWidgets = new ArrayList<>();
    public final Screen parent;
    private final Type type;
    
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    
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
        Type type
    ) {
        super(MinecraftClient.getInstance(), width, height, top, bottom, itemHeight);
        this.parent = parent;
        this.type = type;
        
//        if (type == Type.mainDimensionList) {
//            AltiusScreen parent1 = (AltiusScreen) parent;
//
//            extraLoopButton = new ButtonWidget(
//                0, 0, 100, 20,
//                new TranslatableText(parent1.loopEnabled ?
//                    "imm_ptl.enabled" : "imm_ptl.disabled"),
//                button -> {
//                    parent1.loopEnabled = !parent1.loopEnabled;
//                    button.setMessage(
//                        new TranslatableText(parent1.loopEnabled ?
//                            "imm_ptl.enabled" : "imm_ptl.disabled")
//                    );
//                }
//            );
//        }
    }
    
    public void update() {
        this.clearEntries();
        this.entryWidgets.forEach(this::addEntry);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (type == Type.mainDimensionList) {
            DimEntryWidget selected = getSelectedOrNull();
            
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
    
//    @Override
//    protected int getMaxPosition() {
//        if (type == Type.mainDimensionList) {
//            return super.getMaxPosition() + itemHeight;
//        }
//
//        return super.getMaxPosition();
//    }
    
    @Override
    protected void renderList(MatrixStack matrices, int x, int y, int mouseX, int mouseY, float delta) {
        super.renderList(matrices, x, y, mouseX, mouseY, delta);
        
        if (type == Type.mainDimensionList) {
            renderLoopButton(matrices, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
    }
    
    private void renderLoopButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int localOffset = getMaxPosition() - 35;
        int currY = top + 4 - (int) getScrollAmount() + localOffset;
//        extraLoopButton.y = currY;
//        extraLoopButton.x = getRowLeft() + 100;
//
//        extraLoopButton.render(matrices, mouseX, mouseY, delta);
        
//        new GuiHelper.Rect(
//            getRowLeft() + 30, currY, 200, currY + 100
//        ).renderTextLeft(new TranslatableText("imm_ptl.loop"), matrices);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//        if (type == Type.mainDimensionList) {
//            extraLoopButton.mouseClicked(mouseX, mouseY, button);
//        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
