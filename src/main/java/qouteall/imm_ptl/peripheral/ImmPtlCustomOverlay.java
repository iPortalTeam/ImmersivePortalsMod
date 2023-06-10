package qouteall.imm_ptl.peripheral;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Make this because {@link Gui#setOverlayMessage(Component, boolean)} does not support multi-line
 */
@Environment(EnvType.CLIENT)
public class ImmPtlCustomOverlay {
    @Nullable
    private static MultiLineLabel multiLineLabel;
    private static long clearingTime = 0;
    
    public static void putText(MultiLineLabel label, double durationSeconds) {
        multiLineLabel = label;
        clearingTime = System.nanoTime() + Helper.secondToNano(durationSeconds);
    }
    
    public static void putText(Component component, double durationSeconds) {
        putText(
            MultiLineLabel.create(
                Minecraft.getInstance().font,
                component,
                (Minecraft.getInstance().getWindow().getGuiScaledWidth() - 20)
            ), 0.2
        );
    }
    
    /**
     * {@link Gui#render(GuiGraphics, float)}
     * {@link net.minecraft.client.gui.screens.AlertScreen}
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        if (multiLineLabel == null) {
            return;
        }
        
        if (System.nanoTime() > clearingTime) {
            multiLineLabel = null;
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        guiGraphics.pose().pushPose();
        
        int guiScaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiScaledHeight = minecraft.getWindow().getGuiScaledHeight();
        
        Font font = minecraft.gui.getFont();
        
        minecraft.getProfiler().push("imm_ptl_custom_overlay");
        // Note: the parchment names are incorrect
        multiLineLabel.renderCentered(
            guiGraphics,
            guiScaledWidth / 2, (int) (guiScaledHeight * 0.75)
        );
        guiGraphics.pose().popPose();
        
        minecraft.getProfiler().pop();
    }
}
