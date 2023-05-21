package qouteall.imm_ptl.peripheral;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
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
     * {@link Gui#render(PoseStack, float)}
     * {@link net.minecraft.client.gui.screens.AlertScreen}
     */
    public static void render(PoseStack poseStack, float partialTick) {
        if (multiLineLabel == null) {
            return;
        }
        
        if (System.nanoTime() > clearingTime) {
            multiLineLabel = null;
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        
        poseStack.pushPose();
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        poseStack.translate(0, screenHeight * 0.3, 0);
        
        Font font = minecraft.gui.getFont();
        
        minecraft.getProfiler().push("imm_ptl_custom_overlay");
        multiLineLabel.renderCentered(poseStack, screenWidth / 2, 90);
        poseStack.popPose();
        
        minecraft.getProfiler().pop();
    }
}
