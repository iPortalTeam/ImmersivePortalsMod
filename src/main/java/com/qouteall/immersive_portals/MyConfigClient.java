package com.qouteall.immersive_portals;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class MyConfigClient {
    public int maxPortalLayer = 5;
    public boolean compatibilityRenderMode = false;
    public boolean doCheckGlError = false;
    
    public static MyConfigClient readConfigFromFile() {
        File runDirectory = MinecraftClient.getInstance().runDirectory;
        File configFile = new File(runDirectory, "imm_ptl_config_client.json");
        
        if (configFile.exists()) {
            try {
                String data = Files.lines(configFile.toPath()).collect(Collectors.joining());
                return SGlobal.gson.fromJson(data, MyConfigClient.class);
            }
            catch (IOException e) {
                e.printStackTrace();
                return new MyConfigClient();
            }
        }
        else {
            MyConfigClient configObject = new MyConfigClient();
            saveConfigFile(configObject);
            return configObject;
        }
    }
    
    public static void saveConfigFile(MyConfigClient configObject) {
        File runDirectory = MinecraftClient.getInstance().runDirectory;
        File configFile1 = new File(runDirectory, "imm_ptl_config_client.json");
        try {
            configFile1.createNewFile();
            FileWriter fileWriter = new FileWriter(configFile1);
            
            fileWriter.write(SGlobal.gson.toJson(configObject));
            fileWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void onConfigChanged(MyConfigClient config) {
        if (config.compatibilityRenderMode) {
            CGlobal.renderMode = CGlobal.RenderMode.compatibility;
        }
        else {
            CGlobal.renderMode = CGlobal.RenderMode.normal;
        }
        CGlobal.doCheckGlError = config.doCheckGlError;
        CGlobal.maxPortalLayer = config.maxPortalLayer;
    }
}
