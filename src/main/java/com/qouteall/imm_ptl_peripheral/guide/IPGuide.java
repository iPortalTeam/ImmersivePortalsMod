package com.qouteall.imm_ptl_peripheral.guide;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.network.CommonNetworkClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class IPGuide {
    public static class GuideInfo {
        public boolean wikiInformed = false;
        public boolean portalHelperInformed = false;
        
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
            
            if (!guideInfo.wikiInformed) {
                if (player != null && player.isCreative()) {
                    guideInfo.wikiInformed = true;
                    writeToFile(guideInfo);
                    informWithURL(
                        "https://qouteall.fun/immptl/wiki/Portal-Customization",
                        new TranslatableText("imm_ptl.inform_wiki")
                    );
                }
            }
        });
    }
    
    public static void onClientPlacePortalHelper() {
        if (!guideInfo.portalHelperInformed) {
            guideInfo.portalHelperInformed = true;
            writeToFile(guideInfo);
            
            informWithURL(
                "https://qouteall.fun/immptl/wiki/Portal-Customization#portal-helper-block",
                new TranslatableText("imm_ptl.inform_portal_helper")
            );
        }
    }
    
    private static void informWithURL(String link, MutableText text) {
        MinecraftClient.getInstance().inGameHud.addChatMessage(
            MessageType.SYSTEM,
            text.append(
                McHelper.getLinkText(link)
            ),
            Util.NIL_UUID
        );
    }
    
    @Environment(EnvType.CLIENT)
    public static class RemoteCallables {
        public static void showWiki() {
            informWithURL(
                "https://qouteall.fun/immptl/wiki/Commands-Reference",
                new LiteralText("")
            );
        }
    }
}
