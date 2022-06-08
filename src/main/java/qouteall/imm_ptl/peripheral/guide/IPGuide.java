package qouteall.imm_ptl.peripheral.guide;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.network.IPCommonNetworkClient;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class IPGuide {
    public static class GuideInfo {
        public boolean wikiInformed = false;
        public boolean portalHelperInformed = false;
        public boolean lagInformed = false;
        
        public GuideInfo() {}
    }
    
    private static GuideInfo readFromFile() {
        File storageFile = getStorageFile();
        
        if (storageFile.exists()) {
            
            GuideInfo result = null;
            try (FileReader fileReader = new FileReader(storageFile)) {
                result = IPGlobal.gson.fromJson(fileReader, GuideInfo.class);
            }
            catch (Throwable e) {
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
        return new File(Minecraft.getInstance().gameDirectory, "imm_ptl_state.json");
    }
    
    private static void writeToFile(GuideInfo guideInfo) {
        try (FileWriter fileWriter = new FileWriter(getStorageFile())) {
            
            IPGlobal.gson.toJson(guideInfo, fileWriter);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static GuideInfo guideInfo = new GuideInfo();
    
    public static void initClient() {
        guideInfo = readFromFile();
        
        IPCommonNetworkClient.clientPortalSpawnSignal.connect(p -> {
            LocalPlayer player = Minecraft.getInstance().player;
            
            if (!guideInfo.wikiInformed) {
                if (player != null && player.isCreative()) {
                    guideInfo.wikiInformed = true;
                    writeToFile(guideInfo);
                    informWithURL(
                        "https://qouteall.fun/immptl/wiki/Portal-Customization",
                        Component.translatable("imm_ptl.inform_wiki")
                    );
                }
            }
            
            if (!guideInfo.lagInformed) {
                if (player != null) {
                    guideInfo.lagInformed = true;
                    writeToFile(guideInfo);
                    
                    IPGlobal.clientTaskList.addTask(MyTaskList.withDelay(100, () -> {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.about_lag")
                        );
                        return true;
                    }));
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
                Component.translatable("imm_ptl.inform_portal_helper")
            );
        }
    }
    
    private static void informWithURL(String link, MutableComponent text) {
        CHelper.printChat(
            text.append(
                McHelper.getLinkText(link)
            )
        );
    }
    
    @Environment(EnvType.CLIENT)
    public static class RemoteCallables {
        public static void showWiki() {
            informWithURL(
                "https://qouteall.fun/immptl/wiki/Commands-Reference",
                Component.literal("")
            );
        }
    }
}
