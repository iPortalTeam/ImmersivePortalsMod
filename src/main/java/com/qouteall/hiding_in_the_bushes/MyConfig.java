package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class MyConfig {
    public int maxPortalLayer = 5;
    public boolean compatibilityRenderMode = false;
    public boolean doCheckGlError = false;
    public int portalSearchingRange = 128;
    public boolean longerReachInCreative = true;
    public boolean renderYourselfInPortal = true;
    public boolean activeLoading = true;
    
    private static File getGameDir() {
        if (FabricLoader.INSTANCE.getEnvironmentType() == EnvType.CLIENT) {
            return MinecraftClient.getInstance().runDirectory;
        }
        else {
            return McHelper.getServer().getRunDirectory();
        }
    }
    
    public static MyConfig readConfigFromFile() {
        File configFile = new File(getGameDir(), "imm_ptl_config.json");
        
        if (configFile.exists()) {
            try {
                String data = Files.lines(configFile.toPath()).collect(Collectors.joining());
                return Global.gson.fromJson(data, MyConfig.class);
            }
            catch (IOException e) {
                e.printStackTrace();
                return new MyConfig();
            }
        }
        else {
            MyConfig configObject = new MyConfig();
            saveConfigFile(configObject);
            return configObject;
        }
    }
    
    public static void saveConfigFile(MyConfig configObject) {
        File configFile1 = new File(getGameDir(), "imm_ptl_config.json");
        try {
            configFile1.createNewFile();
            FileWriter fileWriter = new FileWriter(configFile1);
            
            fileWriter.write(Global.gson.toJson(configObject));
            fileWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void onConfigChanged(MyConfig config) {
        if (config.compatibilityRenderMode) {
            Global.renderMode = Global.RenderMode.compatibility;
        }
        else {
            Global.renderMode = Global.RenderMode.normal;
        }
        Global.doCheckGlError = config.doCheckGlError;
        Global.maxPortalLayer = config.maxPortalLayer;
        Global.netherPortalFindingRadius = config.portalSearchingRange;
        Global.longerReachInCreative = config.longerReachInCreative;
        Global.renderYourselfInPortal = config.renderYourselfInPortal;
    
        if (O_O.isReachEntityAttributesPresent) {
            Global.longerReachInCreative = false;
        }
    
        Global.activeLoading = config.activeLoading;
    }
}
