package qouteall.imm_ptl.peripheral.guide;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPNetworkingClient;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackInfo;
import qouteall.q_misc_util.my_util.MyTaskList;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class IPOuterClientMisc {
    public static class OuterConfig {
        public boolean wikiInformed = false;
        public boolean portalHelperInformed = false;
        public boolean lagInformed = false;
        
        // the dimension stack feature is not in imm_ptl_core
        // so put the config here
        @Nullable
        public DimStackInfo dimensionStackDefault = null;
        
        public OuterConfig() {}
    }
    
    private static OuterConfig readFromFile() {
        File storageFile = getStorageFile();
        
        if (storageFile.exists()) {
            
            OuterConfig result = null;
            try (FileReader fileReader = new FileReader(storageFile)) {
                result = IPGlobal.gson.fromJson(fileReader, OuterConfig.class);
            } catch (Throwable e) {
                e.printStackTrace();
                return new OuterConfig();
            }
            
            if (result == null) {
                return new OuterConfig();
            }
            
            return result;
        }
        
        return new OuterConfig();
    }
    
    private static File getStorageFile() {
        return new File(Minecraft.getInstance().gameDirectory, "imm_ptl_state.json");
    }
    
    private static void writeToFile(OuterConfig outerConfig) {
        try (FileWriter fileWriter = new FileWriter(getStorageFile())) {
            
            IPGlobal.gson.toJson(outerConfig, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static OuterConfig outerConfig = new OuterConfig();
    
    public static void initClient() {
        outerConfig = readFromFile();
        
        IPNetworkingClient.clientPortalSpawnSignal.connect(p -> {
            LocalPlayer player = Minecraft.getInstance().player;
            
            if (!outerConfig.wikiInformed) {
                if (player != null && player.isCreative()) {
                    outerConfig.wikiInformed = true;
                    writeToFile(outerConfig);
                    informWithURL(
                        "https://qouteall.fun/immptl/wiki/Portal-Customization",
                        Component.translatable("imm_ptl.inform_wiki")
                    );
                }
            }
            
            if (!outerConfig.lagInformed) {
                if (player != null) {
                    outerConfig.lagInformed = true;
                    writeToFile(outerConfig);
                    
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
        if (!outerConfig.portalHelperInformed) {
            outerConfig.portalHelperInformed = true;
            writeToFile(outerConfig);
            
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
    
    @Nullable
    public static DimStackInfo getDimStackPreset() {
        return outerConfig.dimensionStackDefault;
    }
    
    public static void setDimStackPreset(@Nullable DimStackInfo info) {
        outerConfig.dimensionStackDefault = info;
        writeToFile(outerConfig);
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
