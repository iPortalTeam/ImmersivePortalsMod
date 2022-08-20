package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class IPConfig {
    // json does not allow comments...
    public String check_the_wiki_for_more_information = "https://qouteall.fun/immptl/wiki/Config-Options";
    
    public boolean enableWarning = true;
    public boolean enableMirrorCreation = true;
    public int maxPortalLayer = 5;
    public boolean sharedBlockMeshBufferOptimization = true;
    public boolean lagAttackProof = true;
    public int portalRenderLimit = 200;
    public int indirectLoadingRadiusCap = 8;
    public boolean enableCrossPortalSound = true;
    public boolean compatibilityRenderMode = false;
    public boolean doCheckGlError = false;
    public int portalSearchingRange = 128;
    public boolean renderYourselfInPortal = true;
    public boolean serverSideNormalChunkLoading = true;
    public boolean teleportationDebug = false;
    public boolean correctCrossPortalEntityRendering = true;
    public boolean looseMovementCheck = false;
    public boolean pureMirror = false;
    public boolean enableAlternateDimensions = true;
    public boolean reducedPortalRendering = false;
    public boolean visibilityPrediction = true;
    public boolean netherPortalOverlay = false;
    public int scaleLimit = 30;
    public boolean easeCreativePermission = true;
    public boolean easeCommandStickPermission = false;
    public boolean enableDatapackPortalGen = true;
    public boolean enableCrossPortalView = true;
    public boolean enableClippingMechanism = true;
    public boolean enableDepthClampForPortalRendering = false;
    public boolean lightVanillaNetherPortalWhenCrouching = false;
    public boolean enableNetherPortalEffect = true;
    public boolean enableClientPerformanceAdjustment = true;
    public boolean enableServerPerformanceAdjustment = true;
    public boolean checkModInfoFromInternet = true;
    public IPGlobal.NetherPortalMode netherPortalMode = IPGlobal.NetherPortalMode.normal;
    public IPGlobal.EndPortalMode endPortalMode = IPGlobal.EndPortalMode.normal;
//    public boolean enableServerCollision = true;
    
    private static File getGameDir() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        
        return gameDir.toFile();
    }
    
    public static IPConfig readConfig() {
        File oldConfigFile = new File(getGameDir(), "imm_ptl_config.json");
        File newConfigFile = getConfigFileLocation();
        
        if (oldConfigFile.exists()) {
            Helper.log("Detected the old config file. deleted.");
            IPConfig result = readConfigFromFile(oldConfigFile);
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
    
    public static IPConfig readConfigFromFile(File configFile) {
        if (configFile.exists()) {
            try {
                String data = Files.lines(configFile.toPath()).collect(Collectors.joining());
                IPConfig result = IPGlobal.gson.fromJson(data, IPConfig.class);
                if (result == null) {
                    return new IPConfig();
                }
                return result;
            }
            catch (Throwable e) {
                e.printStackTrace();
                return new IPConfig();
            }
        }
        else {
            IPConfig configObject = new IPConfig();
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
            
            fileWriter.write(IPGlobal.gson.toJson(this));
            fileWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void onConfigChanged() {
        if (compatibilityRenderMode) {
            IPGlobal.renderMode = IPGlobal.RenderMode.compatibility;
        }
        else {
            IPGlobal.renderMode = IPGlobal.RenderMode.normal;
        }
        IPGlobal.enableWarning = enableWarning;
        IPGlobal.enableMirrorCreation = enableMirrorCreation;
        IPGlobal.doCheckGlError = doCheckGlError;
        IPGlobal.maxPortalLayer = maxPortalLayer;
        IPGlobal.lagAttackProof = lagAttackProof;
        IPGlobal.portalRenderLimit = portalRenderLimit;
        IPGlobal.netherPortalFindingRadius = portalSearchingRange;
        IPGlobal.renderYourselfInPortal = renderYourselfInPortal;
        IPGlobal.activeLoading = serverSideNormalChunkLoading;
        IPGlobal.teleportationDebugEnabled = teleportationDebug;
        IPGlobal.correctCrossPortalEntityRendering = correctCrossPortalEntityRendering;
        IPGlobal.looseMovementCheck = looseMovementCheck;
        IPGlobal.pureMirror = pureMirror;
        IPGlobal.enableAlternateDimensions = enableAlternateDimensions;
        IPGlobal.indirectLoadingRadiusCap = indirectLoadingRadiusCap;
        IPGlobal.netherPortalMode = netherPortalMode;
        IPGlobal.endPortalMode = endPortalMode;
        IPGlobal.reducedPortalRendering = reducedPortalRendering;
        IPGlobal.offsetOcclusionQuery = visibilityPrediction;
        IPGlobal.netherPortalOverlay = netherPortalOverlay;
        IPGlobal.scaleLimit = scaleLimit;
        IPGlobal.easeCreativePermission = easeCreativePermission;
        IPGlobal.enableSharedBlockMeshBuffers = sharedBlockMeshBufferOptimization;
        IPGlobal.enableDatapackPortalGen = enableDatapackPortalGen;
        IPGlobal.enableCrossPortalView = enableCrossPortalView;
        IPGlobal.enableClippingMechanism = enableClippingMechanism;
        IPGlobal.lightVanillaNetherPortalWhenCrouching = lightVanillaNetherPortalWhenCrouching;
        IPGlobal.enableNetherPortalEffect = enableNetherPortalEffect;
        IPGlobal.enableClientPerformanceAdjustment = enableClientPerformanceAdjustment;
        IPGlobal.enableServerPerformanceAdjustment = enableServerPerformanceAdjustment;
        IPGlobal.enableCrossPortalSound = enableCrossPortalSound;
        IPGlobal.checkModInfoFromInternet = checkModInfoFromInternet;
        
        if (enableDepthClampForPortalRendering) {
            IPGlobal.enableDepthClampForPortalRendering = true;
        }
        
        Helper.log("IP Config Applied");
        
    }
    
}
