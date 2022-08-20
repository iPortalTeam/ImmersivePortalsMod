package qouteall.imm_ptl.core.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class IPModCompatibilityWarning {
    
    // GSON does not support records https://github.com/google/gson/issues/1794
    public static final class ModEntry {
        public String modId;
        public String modName;
        @Nullable
        public String startVersion;
        @Nullable
        public String endVersion;
        
        public ModEntry(
            String modId,
            String modName,
            @Nullable String startVersion,
            @Nullable String endVersion
        ) {
            this.modId = modId;
            this.modName = modName;
            this.startVersion = startVersion;
            this.endVersion = endVersion;
        }
        
        boolean isModLoadedWithinVersion() {
            return O_O.isModLoadedWithinVersion(modId, startVersion, endVersion);
        }
        
        String getVersionRangeStr() {
            if (startVersion != null) {
                if (endVersion != null) {
                    return startVersion + "-" + endVersion;
                }
                else {
                    return startVersion + "+";
                }
            }
            else {
                Validate.notNull(endVersion);
                return "-" + endVersion;
            }
        }
        
        @Override
        public String toString() {
            return "ModEntry[" +
                "modId=" + modId + ", " +
                "modName=" + modName + ", " +
                "startVersion=" + startVersion + ", " +
                "endVersion=" + endVersion + ']';
        }
        
    }
    
    public static final class LatestReleaseInfo {
        public String modVersion;
        public String mcVersion;
        
        public LatestReleaseInfo(
            String modVersion, String mcVersion
        ) {
            this.modVersion = modVersion;
            this.mcVersion = mcVersion;
        }
        
        @Override
        public String toString() {
            return "LatestReleaseInfo[" +
                "modVersion=" + modVersion + ", " +
                "mcVersion=" + mcVersion + ']';
        }
        
    }
    
    public static final class ImmPtlInfo {
        public LatestReleaseInfo latestRelease;
        public List<ModEntry> severelyIncompatible;
        public List<ModEntry> incompatible;
        
        public ImmPtlInfo(
            LatestReleaseInfo latestRelease,
            List<ModEntry> severelyIncompatible,
            List<ModEntry> incompatible
        ) {
            this.latestRelease = latestRelease;
            this.severelyIncompatible = severelyIncompatible;
            this.incompatible = incompatible;
        }
        
        @Override
        public String toString() {
            return "ImmPtlInfo[" +
                "latestRelease=" + latestRelease + ", " +
                "severelyIncompatible=" + severelyIncompatible + ", " +
                "incompatible=" + incompatible + ']';
        }
    }
    
    // NOTE do not run it on render thread
    @Nullable
    public static ImmPtlInfo fetchImmPtlInfoFromInternet() {
        String url = O_O.getImmPtlModInfoUrl();
        
        if (url == null) {
            Helper.log("Not fetching immptl mod info");
            return null;
        }
        
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(url);
            request.addHeader(HttpHeaders.USER_AGENT, "Immersive Portals mod");
            
            try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    Helper.log("Failed to fetch immptl mod info " + statusCode);
                    return null;
                }
                
                HttpEntity entity = httpResponse.getEntity();
                
                if (entity == null) {
                    return null;
                }
                
                String jsonStr = EntityUtils.toString(entity);
                ImmPtlInfo immPtlInfo = Helper.gson.fromJson(jsonStr, ImmPtlInfo.class);
                return immPtlInfo;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void initDedicatedServer() {
        Util.backgroundExecutor().execute(() -> {
            if (!IPGlobal.checkModInfoFromInternet) {
                return;
            }
            
            ImmPtlInfo immPtlInfo = fetchImmPtlInfoFromInternet();
            
            if (immPtlInfo == null) {
                return;
            }
            
            if (O_O.shouldUpdateImmPtl(immPtlInfo.latestRelease.modVersion)) {
                Helper.log("A new version of Immersive Portals is available: %s (for MC %s)".formatted(
                    immPtlInfo.latestRelease.modVersion,
                    immPtlInfo.latestRelease.mcVersion
                ));
            }
            
            for (ModEntry mod : immPtlInfo.severelyIncompatible) {
                if (mod.isModLoadedWithinVersion()) {
                    Helper.err(String.format(
                        "ERROR: This mod is incompatible with Immersive Portals: %s(%s). Severe issues will occur!" +
                            " (If the two mods become compatible, contact qouteall)",
                        mod.modName, mod.modId
                    ));
                }
            }
            
            for (ModEntry mod : immPtlInfo.incompatible) {
                if (mod.isModLoadedWithinVersion()) {
                    Helper.err(String.format(
                        "WARNING: This mod has compatibility issues with Immersive Portals: %s(%s)",
                        mod.modName, mod.modId
                    ));
                }
            }
        });
        
        
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        Util.backgroundExecutor().execute(() -> {
            if (!IPGlobal.checkModInfoFromInternet) {
                return;
            }
            
            ImmPtlInfo immPtlInfo = fetchImmPtlInfoFromInternet();
            
            if (immPtlInfo == null) {
                return;
            }
            
            IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
                () -> Minecraft.getInstance().level == null,
                MyTaskList.oneShotTask(() -> {
                    if (O_O.shouldUpdateImmPtl(immPtlInfo.latestRelease.modVersion)) {
                        CHelper.printChat(Component.translatable(
                            "imm_ptl.new_version_available",
                            immPtlInfo.latestRelease.modVersion,
                            immPtlInfo.latestRelease.mcVersion
                        ).append(McHelper.getLinkText(O_O.getModDownloadLink())));
                    }
                    
                    for (ModEntry mod : immPtlInfo.severelyIncompatible) {
                        if (mod != null && mod.isModLoadedWithinVersion()) {
                            if (mod.startVersion != null || mod.endVersion != null) {
                                CHelper.printChat(Component.translatable(
                                    "imm_ptl.severely_incompatible_within_version", mod.modName, mod.modId,
                                    mod.getVersionRangeStr()
                                ).withStyle(ChatFormatting.RED));
                            }
                            else {
                                CHelper.printChat(Component.translatable(
                                    "imm_ptl.severely_incompatible", mod.modName, mod.modId
                                ).withStyle(ChatFormatting.RED));
                            }
                        }
                    }
                    
                    for (ModEntry mod : immPtlInfo.incompatible) {
                        if (mod != null && mod.isModLoadedWithinVersion()) {
                            String warningMessage = String.format(
                                "WARNING: Immersive Portals mod maybe has compatibility issues with mod %s(%s).",
                                mod.modName, mod.modId
                            );
                            if (IPGlobal.enableWarning) {
                                CHelper.printChat(
                                    Component.literal(warningMessage).withStyle(ChatFormatting.RED)
                                        .append(IPMcHelper.getDisableWarningText())
                                );
                            }
                            Helper.err(warningMessage);
                        }
                    }
                })
            ));
            
            
        });
        
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPGlobal.enableWarning && !FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    List<ModContainer> topLevelMods = FabricLoader.getInstance().getAllMods().stream()
                        .filter(modContainer -> modContainer.getContainingMod().isEmpty())
                        .collect(Collectors.toList());
                    
                    if (topLevelMods.size() > 20) {
                        CHelper.printChat(Component.literal(
                            "[Immersive Portals] WARNING: You are using many mods. It's likely that one of them has compatibility issues with Immersive Portals. " +
                                "If you are sure that there is no compatibility issue, disable this warning."
                        ).withStyle(ChatFormatting.RED).append(IPMcHelper.getDisableWarningText()));
                    }
                }
            })
        ));
        
    }
}
