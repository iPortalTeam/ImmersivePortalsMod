package qouteall.q_misc_util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.ProfilerCompat;

import java.util.List;
import java.util.TreeMap;

/**
 * Make this because {@link Gui#setOverlayMessage(Component, boolean)} does not support multi-line
 */
@Environment(EnvType.CLIENT)
public class CustomTextOverlay {
    
    public static record Entry(
        Component component,
        long clearingTime
    ) {}
    
    private static final TreeMap<String, Entry> ENTRIES = new TreeMap<>();
    
    private static final boolean renderAtBottomCenter = true;
    
    @Nullable
    private static List<FormattedCharSequence> lineCache;
    
    public static void putText(Component component, double durationSeconds, String key) {
        ENTRIES.put(
            key,
            new Entry(
                component,
                System.nanoTime() + Helper.secondToNano(durationSeconds)
            )
        );
        lineCache = null;
    }
    
    public static void putText(Component component, double durationSeconds) {
        putText(component, durationSeconds, "5_defaultKey");
    }
    
    public static void putText(Component component, String key) {
        putText(component, 0.2, key);
    }
    
    public static void putText(Component component) {
        putText(component, 0.2, "5_defaultKey");
    }
    
    public static boolean remove(String key) {
        return ENTRIES.remove(key) != null;
    }
    
    /**
     * {@link Gui#render(GuiGraphics, float)}
     * {@link net.minecraft.client.gui.screens.AlertScreen}
     */
    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        long currTime = System.nanoTime();
        
        boolean removes = ENTRIES.entrySet().removeIf(e -> e.getValue().clearingTime < currTime);
        if (removes) {
            lineCache = null;
        }
        
        if (ENTRIES.isEmpty()) {
            return;
        }
        
        if (lineCache == null) {
            // don't make the first component the base component
            // to avoid style override
            MutableComponent component = Component.empty();
            boolean isBeginning = true;
            for (Entry entry : ENTRIES.values()) {
                if (isBeginning) {
                    isBeginning = false;
                }
                else {
                    component.append("\n");
                }
                component.append(entry.component());
            }

            Minecraft minecraft = Minecraft.getInstance();
            Font font = minecraft.font;
            int maxWidth = minecraft.getWindow().getGuiScaledWidth() - 20;
            lineCache = font.split(component, maxWidth);
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        guiGraphics.pose().pushMatrix();
        
        int guiScaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiScaledHeight = minecraft.getWindow().getGuiScaledHeight();
        
        Font font = minecraft.gui.getFont();
        
        ProfilerCompat.push("imm_ptl_custom_overlay");
        int lineHeight = font.lineHeight;
        int color = 0xffffffff;
        int baseX = renderAtBottomCenter ? guiScaledWidth / 2 : 10;
        int baseY = renderAtBottomCenter ? (int) (guiScaledHeight * 0.75) : 10;
        
        if (lineCache != null) {
            int y = baseY;
            for (FormattedCharSequence line : lineCache) {
                int x = renderAtBottomCenter ? baseX - font.width(line) / 2 : baseX;
                guiGraphics.drawString(font, line, x, y, color);
                y += lineHeight;
            }
        }
        
        guiGraphics.pose().popMatrix();
        
        ProfilerCompat.pop();
    }
}
