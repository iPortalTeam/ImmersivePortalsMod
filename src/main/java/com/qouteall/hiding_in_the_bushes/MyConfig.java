package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyConfig {
    
    public int maxPortalLayer = 5;
    public boolean compatibilityRenderMode = false;
    public boolean doCheckGlError = false;
    public int portalSearchingRange = 128;
    public boolean longerReachInCreative = true;
    public boolean renderYourselfInPortal = true;
    public boolean activeLoading = true;
    public boolean teleportationDebug = false;
    public boolean correctCrossPortalEntityRendering = true;
    public boolean loadFewerChunks = false;
    public boolean multiThreadedNetherPortalSearching = true;
    public boolean edgelessSky = false;
    public boolean reversibleNetherPortalLinking = false;
    public boolean mirrorInteractableThroughPortal = true;
    public Map<String, String> dimensionRenderRedirect = defaultRedirectMap;
    
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
            configObject.saveConfigFile();
            return configObject;
        }
    }
    
    public void saveConfigFile() {
        File configFile1 = new File(getGameDir(), "imm_ptl_config.json");
        try {
            configFile1.createNewFile();
            FileWriter fileWriter = new FileWriter(configFile1);
            
            fileWriter.write(Global.gson.toJson(this));
            fileWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void onConfigChanged() {
        if (compatibilityRenderMode) {
            Global.renderMode = Global.RenderMode.compatibility;
        }
        else {
            Global.renderMode = Global.RenderMode.normal;
        }
        Global.doCheckGlError = doCheckGlError;
        Global.maxPortalLayer = maxPortalLayer;
        Global.netherPortalFindingRadius = portalSearchingRange;
        Global.longerReachInCreative = longerReachInCreative;
        Global.renderYourselfInPortal = renderYourselfInPortal;

        if (O_O.isReachEntityAttributesPresent) {
            Global.longerReachInCreative = false;
        }
        
        Global.activeLoading = activeLoading;
        Global.teleportationDebugEnabled = teleportationDebug;
        Global.correctCrossPortalEntityRendering = correctCrossPortalEntityRendering;
        Global.loadFewerChunks = loadFewerChunks;
        Global.multiThreadedNetherPortalSearching = multiThreadedNetherPortalSearching;
        Global.edgelessSky = edgelessSky;
        Global.reversibleNetherPortalLinking = reversibleNetherPortalLinking;
        Global.mirrorInteractableThroughPortal = mirrorInteractableThroughPortal;

        if (FabricLoader.INSTANCE.getEnvironmentType() == EnvType.CLIENT) {
            RenderDimensionRedirect.updateIdMap(dimensionRenderRedirect);
        }
    }
    
    public static Map<String, String> listToMap(List<String> redirectList) {
        Map<String, String> result = new HashMap<>();
        for (String s : redirectList) {
            int i = s.indexOf(splitter);
            if (i != -1) {
                result.put(
                    s.substring(0, i),
                    s.substring(i + 2)
                );
            }
            else {
                result.put(s, "???");
            }
        }
        return result;
    }
    
    public static List<String> mapToList(Map<String, String> redirectMap) {
        return redirectMap.entrySet().stream()
            .map(entry -> entry.getKey() + splitter + entry.getValue())
            .collect(Collectors.toList());
    }
    
    private static final String splitter = "->";
    private static final Map<String, String> defaultRedirectMap = new HashMap<>();
    public static final List<String> defaultRedirectMapList;
    
    static {
        defaultRedirectMap.put("immersive_portals:alternate1", "minecraft:overworld");
        defaultRedirectMap.put("immersive_portals:alternate2", "minecraft:overworld");
        defaultRedirectMap.put("immersive_portals:alternate3", "minecraft:overworld");
        defaultRedirectMap.put("immersive_portals:alternate4", "minecraft:overworld");
        defaultRedirectMap.put("immersive_portals:alternate5", "minecraft:overworld");
        
        defaultRedirectMapList = mapToList(defaultRedirectMap);
    }
}
