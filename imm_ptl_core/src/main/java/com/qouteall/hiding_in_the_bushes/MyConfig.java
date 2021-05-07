package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
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
    // json does not allow comments...
    public String check_the_wiki_for_more_information = "https://qouteall.fun/immptl/wiki/Config-Options";
    
    public int maxPortalLayer = 5;
    public boolean lagAttackProof = true;
    public int portalRenderLimit = 200;
    public int indirectLoadingRadiusCap = 8;
    public boolean compatibilityRenderMode = false;
    public boolean doCheckGlError = false;
    public int portalSearchingRange = 128;
    public boolean renderYourselfInPortal = true;
    public boolean activeLoading = true;
    public boolean teleportationDebug = false;
    public boolean correctCrossPortalEntityRendering = true;
    public boolean multiThreadedNetherPortalSearching = true;
    public boolean edgelessSky = false;
    public boolean looseMovementCheck = false;
    public boolean pureMirror = false;
    public boolean enableAlternateDimensions = true;
    public boolean reducedPortalRendering = false;
    public boolean visibilityPrediction = true;
    public int chunkUnloadDelayTicks = 15 * 20;
    public boolean forceMergePortalRendering = false;
    public boolean netherPortalOverlay = false;
    public boolean graduallyIncreaseLoadingRange = true;
    public int scaleLimit = 30;
    public boolean easeCreativePermission = true;
    public boolean easeCommandStickPermission = false;
    public Map<String, String> dimensionRenderRedirect = defaultRedirectMap;
    public Global.NetherPortalMode netherPortalMode = Global.NetherPortalMode.normal;
    public Global.EndPortalMode endPortalMode = Global.EndPortalMode.normal;
    
    private static File getGameDir() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return MinecraftClient.getInstance().runDirectory;
        }
        else {
            return McHelper.getServer().getRunDirectory();
        }
    }
    
    public static MyConfig readConfig() {
        File oldConfigFile = new File(getGameDir(), "imm_ptl_config.json");
        File newConfigFile = getConfigFileLocation();
        
        if (oldConfigFile.exists()) {
            Helper.log("Detected the old config file. deleted.");
            MyConfig result = readConfigFromFile(oldConfigFile);
            oldConfigFile.delete();
            return result;
        }
        else {
            return readConfigFromFile(newConfigFile);
        }
    }
    
    public static File getConfigFileLocation() {
        return new File(
            getGameDir(), "config/immersive_portals_fabric.json"
        );
    }
    
    public static MyConfig readConfigFromFile(File configFile) {
        if (configFile.exists()) {
            try {
                String data = Files.lines(configFile.toPath()).collect(Collectors.joining());
                MyConfig result = Global.gson.fromJson(data, MyConfig.class);
                if (result == null) {
                    return new MyConfig();
                }
                return result;
            }
            catch (Throwable e) {
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
        File configFile1 = getConfigFileLocation();
        try {
            configFile1.getParentFile().mkdirs();
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
        Global.lagAttackProof = lagAttackProof;
        Global.portalRenderLimit = portalRenderLimit;
        Global.netherPortalFindingRadius = portalSearchingRange;
        Global.renderYourselfInPortal = renderYourselfInPortal;
        
        Global.activeLoading = activeLoading;
        Global.teleportationDebugEnabled = teleportationDebug;
        Global.correctCrossPortalEntityRendering = correctCrossPortalEntityRendering;
        Global.multiThreadedNetherPortalSearching = multiThreadedNetherPortalSearching;
        Global.edgelessSky = edgelessSky;
        Global.looseMovementCheck = looseMovementCheck;
        Global.pureMirror = pureMirror;
        Global.enableAlternateDimensions = enableAlternateDimensions;
        Global.indirectLoadingRadiusCap = indirectLoadingRadiusCap;
        Global.netherPortalMode = netherPortalMode;
        Global.endPortalMode = endPortalMode;
        Global.reducedPortalRendering = reducedPortalRendering;
        Global.offsetOcclusionQuery = visibilityPrediction;
        Global.chunkUnloadDelayTicks = chunkUnloadDelayTicks;
        Global.forceMergePortalRendering = forceMergePortalRendering;
        Global.netherPortalOverlay = netherPortalOverlay;
        Global.serverSmoothLoading = graduallyIncreaseLoadingRange;
        Global.scaleLimit = scaleLimit;
        Global.easeCreativePermission = easeCreativePermission;
        
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            RenderDimensionRedirect.updateIdMap(dimensionRenderRedirect);
        }
        
        Helper.log("IP Config Applied");
        
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
