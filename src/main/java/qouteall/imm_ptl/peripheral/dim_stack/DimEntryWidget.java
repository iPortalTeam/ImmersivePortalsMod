package qouteall.imm_ptl.peripheral.dim_stack;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.McHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// extending EntryListWidget.Entry is also fine
public class DimEntryWidget extends ContainerObjectSelectionList.Entry<DimEntryWidget> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static enum ArrowType {
        none, enabled, conflicting
    }
    
    public final ResourceKey<Level> dimension;
    public final DimListWidget parent;
    private final Consumer<DimEntryWidget> selectCallback;
    @Nullable
    private final Identifier dimIconPath;
    private final Component dimensionName;
    
    // if null, it's in select dimension screen
    // if not null, it's in dim stack screen
    @Nullable
    public final DimStackEntry entry;
    
    public int entryIndex;
    
    ArrowType arrowToPrevious = ArrowType.none;
    ArrowType arrowToNext = ArrowType.none;
    
    public final static int widgetHeight = 50;
    
    @Override
    public @NotNull List<? extends NarratableEntry> narratables() {
        return List.of();
    }
    
    public DimEntryWidget(
        ResourceKey<Level> dimension,
        DimListWidget parent,
        Consumer<DimEntryWidget> selectCallback,
        @Nullable DimStackEntry entry
    ) {
        this.dimension = dimension;
        this.parent = parent;
        this.selectCallback = selectCallback;
        
        this.dimIconPath = CHelper.getDimensionIconPath(this.dimension);
        
        this.dimensionName = McHelper.getDimensionName(dimension);
        
        this.entry = entry;
    }
    
    private final List<GuiEventListener> children = new ArrayList<>();
    
    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return children;
    }
    
    @Override
    public void renderContent(
        @NotNull GuiGraphics guiGraphics,
        int mouseX,
        int mouseY,
        boolean bl,
        float delta
    ) {
        int x = getContentX();
        int y = getContentY();
        int rowWidth = getContentWidth();
        Minecraft client = Minecraft.getInstance();
        
        guiGraphics.drawString(
            client.font, dimensionName.getString(),
            x + widgetHeight + 3, (int) (y),
            0xFFFFFFFF
        );
        
        guiGraphics.drawString(
            client.font, dimension.identifier().toString(),
            x + widgetHeight + 3, (int) (y + 10),
            0xFF999999
        );
        
        if (dimIconPath != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float) x, (float) y);
            
            int iconLen = widgetHeight - 4;
            
            if (entry != null && entry.flipped) {
                guiGraphics.pose().rotateAbout((float) Math.PI, iconLen / 2.0f, iconLen / 2.0f);
            }
            
            guiGraphics.blit(
                dimIconPath, 0, 0, 0, 0,
                iconLen, iconLen,
                iconLen, iconLen
            );
            
            guiGraphics.pose().popMatrix();
        }
        
        if (entry != null) {
            guiGraphics.drawString(
                client.font, getText1(),
                x + widgetHeight + 3, (int) (y + 20),
                0xFF999999
            );
            guiGraphics.drawString(
                client.font, getText2(),
                x + widgetHeight + 3, (int) (y + 30),
                0xFF999999
            );
            
            if (arrowToPrevious != ArrowType.none) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate((float) (x + rowWidth - 13), (float) y);
                guiGraphics.pose().scale(1.5f, 1.5f);
                guiGraphics.drawString(
                    client.font, Component.literal("↑"),
                    0, 0,
                    arrowToPrevious == ArrowType.enabled ? 0xFF999999 : 0xFFFF0000
                );
                guiGraphics.pose().popMatrix();
            }
            
            if (arrowToNext != ArrowType.none) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate((float) (x + rowWidth - 13), y + widgetHeight - 14.5f);
                guiGraphics.pose().scale(1.5f, 1.5f);
                guiGraphics.drawString(
                    client.font, Component.literal("↓"),
                    0, 0,
                    arrowToNext == ArrowType.enabled ? 0xFF999999 : 0xFFFF0000
                );
                guiGraphics.pose().popMatrix();
            }
        }
    }
    
    private Component getText1() {
        MutableComponent scaleText = entry.scale != 1.0 ?
            Component.translatable("imm_ptl.scale")
                .append(Component.literal(":" + Double.toString(entry.scale)))
            : Component.literal("");
        
        return scaleText;
    }
    
    private Component getText2() {
        MutableComponent horizontalRotationText = entry.horizontalRotation != 0 ?
            Component.translatable("imm_ptl.horizontal_rotation")
                .append(Component.literal(":" + Double.toString(entry.horizontalRotation)))
                .append(Component.literal(" "))
            : Component.literal("");
        
        return horizontalRotationText;
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean bl) {
        selectCallback.accept(this);
        super.mouseClicked(event, bl);
        return true;//allow outer dragging
    }
    
}
