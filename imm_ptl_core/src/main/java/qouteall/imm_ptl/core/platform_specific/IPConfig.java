package qouteall.imm_ptl.core.platform_specific;

import com.google.gson.JsonObject;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.Helper;

import java.util.HashSet;

@Config(name = "immersive_portals")
public class IPConfig implements ConfigData {
    // json does not allow comments...
    @ConfigEntry.Gui.Excluded
    public String check_the_wiki_for_more_information = "https://qouteall.fun/immptl/wiki/Config-Options";
    
    // client visible configs
    
    @ConfigEntry.Category("client")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10)
    public int maxPortalLayer = 5;
    @ConfigEntry.Category("client")
    public boolean lagAttackProof = true;
    @ConfigEntry.Category("client")
    public boolean compatibilityRenderMode = false;
    @ConfigEntry.Category("client")
    public boolean enableMirrorCreation = true;
    @ConfigEntry.Category("client")
    public boolean enableCrossPortalSound = true;
    @ConfigEntry.Category("client")
    public boolean pureMirror = false;
    @ConfigEntry.Category("client")
    public boolean renderYourselfInPortal = true;
    @ConfigEntry.Category("client")
    public boolean correctCrossPortalEntityRendering = true;
    @ConfigEntry.Category("client")
    public boolean reducedPortalRendering = false;
    @ConfigEntry.Category("client")
    public boolean netherPortalOverlay = false;
    @ConfigEntry.Category("client")
    public boolean enableNetherPortalEffect = true;
    @ConfigEntry.Category("client")
    public boolean enableClientPerformanceAdjustment = true;
    
    // client invisible configs
    
    @ConfigEntry.Gui.Excluded
    public boolean checkModInfoFromInternet = true;
    @ConfigEntry.Gui.Excluded
    public boolean enableUpdateNotification = true;
    @ConfigEntry.Gui.Excluded
    public boolean sharedBlockMeshBufferOptimization = true;
    @ConfigEntry.Gui.Excluded
    public boolean enableClippingMechanism = true;
    @ConfigEntry.Gui.Excluded
    public boolean visibilityPrediction = true;
    @ConfigEntry.Gui.Excluded
    public boolean useDepthClampForPortalRendering = true;
    @ConfigEntry.Gui.Excluded
    public boolean enableCrossPortalView = true;
    @ConfigEntry.Gui.Excluded
    public int portalRenderLimit = 200;
    @ConfigEntry.Gui.Excluded
    public boolean doCheckGlError = false;
    @ConfigEntry.Gui.Excluded
    public boolean shaderpackWarning = true;
    @ConfigEntry.Gui.Excluded
    public int portalWandCursorAlignment = 2; // zero for no align
    
    // common visible configs
    
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public IPGlobal.NetherPortalMode netherPortalMode = IPGlobal.NetherPortalMode.normal;
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public IPGlobal.EndPortalMode endPortalMode = IPGlobal.EndPortalMode.normal;
    public boolean enableWarning = true;
    public boolean lightVanillaNetherPortalWhenCrouching = true;
    public boolean enableAlternateDimensions = true;
    public boolean enableServerPerformanceAdjustment = true;
    public boolean enableDatapackPortalGen = true;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
    public int indirectLoadingRadiusCap = 8;
    @ConfigEntry.BoundedDiscrete(min = 3, max = 64)
    public int regularPortalLengthLimit = 64;
    @ConfigEntry.BoundedDiscrete(min = 4, max = 128)
    public int scaleLimit = 30;
    public boolean easeCreativePermission = true;
    public boolean easeCommandStickPermission = false;
    public boolean portalsChangeGravityByDefault = false;
    
    // common invisible configs
    
    @ConfigEntry.Gui.Excluded
    public int portalSearchingRange = 128;
    @ConfigEntry.Gui.Excluded
    public boolean serverSideNormalChunkLoading = true;
    @ConfigEntry.Gui.Excluded
    public boolean teleportationDebug = false;
    @ConfigEntry.Gui.Excluded
    public boolean looseMovementCheck = false;
    @ConfigEntry.Gui.Excluded
    public boolean chunkPacketDebug = false;
    @ConfigEntry.Gui.Excluded
    public boolean enableImmPtlChunkLoading = true;
    @ConfigEntry.Gui.Excluded
    public HashSet<String> disabledWarnings = new HashSet<>();
    
    @ConfigEntry.Gui.Excluded
    @Nullable
    public JsonObject dimStackPreset = null;
    
    public static IPConfig getConfig() {
        return IPGlobal.configHolder.getConfig();
    }
    
    public void saveConfigFile() {
        IPGlobal.configHolder.setConfig(this);
        IPGlobal.configHolder.save();
    }
    
    public void onConfigChanged() {
        indirectLoadingRadiusCap = Mth.clamp(indirectLoadingRadiusCap, 1, 32);
        regularPortalLengthLimit = Mth.clamp(regularPortalLengthLimit, 3, 64);
        scaleLimit = Mth.clamp(scaleLimit, 8, 128);
        if (netherPortalMode == null) {
            netherPortalMode = IPGlobal.NetherPortalMode.normal;
        }
        if (endPortalMode == null) {
            endPortalMode = IPGlobal.EndPortalMode.normal;
        }
        
        IPGlobal.renderMode = compatibilityRenderMode ? IPGlobal.RenderMode.compatibility : IPGlobal.RenderMode.normal;
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
        IPGlobal.enableUpdateNotification = enableUpdateNotification;
        IPGlobal.enableDepthClampForPortalRendering = useDepthClampForPortalRendering;
        BlockPortalShape.defaultLengthLimit = regularPortalLengthLimit;
        IPGlobal.maxNormalPortalRadius = Math.max(regularPortalLengthLimit / 2, 16);
        IPGlobal.chunkPacketDebug = chunkPacketDebug;
        
        Helper.log("IP Config Applied");
        
    }
    
    public boolean shouldDisplayWarning(String warningKey) {
        return enableWarning && !disabledWarnings.contains(warningKey);
    }
    
}
