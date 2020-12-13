package com.qouteall.imm_ptl_peripheral.guide;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.network.CommonNetworkClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class IPGuide {
    public static class GuideInfo {
        public boolean wikiInformed = false;
        
        public GuideInfo() {}
    }
    
    private static GuideInfo readFromFile() {
        File storageFile = getStorageFile();
        
        if (storageFile.exists()) {
            
            GuideInfo result = null;
            try (FileReader fileReader = new FileReader(storageFile)) {
                result = Global.gson.fromJson(fileReader, GuideInfo.class);
            }
            catch (IOException e) {
                e.printStackTrace();
                return new GuideInfo();
            }
            
            if (result == null) {
                return new GuideInfo();
            }
            
            return result;
        }
        
        return new GuideInfo();
    }
    
    @NotNull
    private static File getStorageFile() {
        return new File(MinecraftClient.getInstance().runDirectory, "imm_ptl_state.json");
    }
    
    private static void writeToFile(GuideInfo guideInfo) {
        try (FileWriter fileWriter = new FileWriter(getStorageFile())) {
            
            Global.gson.toJson(guideInfo, fileWriter);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static GuideInfo guideInfo = new GuideInfo();
    
    public static void initClient() {
        guideInfo = readFromFile();
        
        CommonNetworkClient.clientPortalSpawnSignal.connect(p -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            
            if (!getIsWikiInformed()) {
                if (player != null && player.isCreative()) {
                    setIsWikiInformed(true);
                    informCustomizePortal();
                }
            }
        });
    }
    
    public static boolean getIsWikiInformed() {
        return guideInfo.wikiInformed;
    }
    
    public static void setIsWikiInformed(boolean cond) {
        guideInfo.wikiInformed = cond;
        writeToFile(guideInfo);
    }
    
    public static void informCustomizePortal() {
        String link = "https://qouteall.fun/immptl/wiki/Portal-Customization";
        MinecraftClient.getInstance().inGameHud.addChatMessage(
            MessageType.SYSTEM,
            new TranslatableText("imm_ptl.inform_wiki").append(
                new LiteralText(link).styled(
                    style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.OPEN_URL, link
                    )).withUnderline(true)
                )
            ),
            Util.NIL_UUID
        );
    }
}
