package qouteall.imm_ptl.core.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import java.util.stream.Collectors;

public class IPModInfoChecking {
    
    public static record ModIncompatInfo(
        String modId,
        String modName,
        @Nullable String startVersion,
        @Nullable String endVersion,
        @Nullable String desc,
        @Nullable String link
    ) {
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
        
    }
    
    public static final class ImmPtlInfo {
        public String latestReleaseVersion;
        public List<ModIncompatInfo> severelyIncompatible;
        public List<ModIncompatInfo> incompatible;
        
        public ImmPtlInfo(String latestReleaseVersion, List<ModIncompatInfo> severelyIncompatible, List<ModIncompatInfo> incompatible) {
            this.latestReleaseVersion = latestReleaseVersion;
            this.severelyIncompatible = severelyIncompatible;
            this.incompatible = incompatible;
        }
        
        @Override
        public String toString() {
            return "ImmPtlInfo{" +
                "latestReleaseVersion='" + latestReleaseVersion + '\'' +
                ", severelyIncompatible=" + severelyIncompatible +
                ", incompatible=" + incompatible +
                '}';
        }
    }
    
    // NOTE do not run it on render thread
    @Nullable
    @Environment(EnvType.CLIENT)
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
                    Helper.err("Failed to fetch immptl mod info " + statusCode);
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
        }
        catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void initDedicatedServer() {
        // currently not doing it in dedicated server
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
                    if (IPGlobal.enableUpdateNotification) {
                        if (O_O.shouldUpdateImmPtl(immPtlInfo.latestReleaseVersion)) {
                            MutableComponent text = Component.translatable(
                                "imm_ptl.new_version_available",
                                immPtlInfo.latestReleaseVersion
                            );
                            text.append(McHelper.getLinkText(O_O.getModDownloadLink()));
                            
                            text.append(Component.literal("  "));
                            text.append(IPMcHelper.getDisableUpdateCheckText());
                            
                            CHelper.printChat(text);
                        }
                    }
                    
                    for (ModIncompatInfo mod : immPtlInfo.severelyIncompatible) {
                        if (mod != null && mod.isModLoadedWithinVersion()) {
                            MutableComponent text;
                            if (mod.startVersion != null || mod.endVersion != null) {
                                text = Component.translatable(
                                    "imm_ptl.severely_incompatible_within_version",
                                    mod.modName, mod.modId,
                                    mod.getVersionRangeStr()
                                ).withStyle(ChatFormatting.RED);
                            }
                            else {
                                text = Component.translatable("imm_ptl.severely_incompatible", mod.modName, mod.modId)
                                    .withStyle(ChatFormatting.RED);
                            }
                            
                            if (mod.desc != null) {
                                text.append(Component.literal(" " + mod.desc + " "));
                            }
                            
                            if (mod.link != null) {
                                text.append(Component.literal(" "));
                                text.append(McHelper.getLinkText(mod.link));
                            }
                            
                            CHelper.printChat(text);
                        }
                    }
                    
                    for (ModIncompatInfo mod : immPtlInfo.incompatible) {
                        if (mod != null && mod.isModLoadedWithinVersion()) {
                            if (IPGlobal.enableWarning) {
                                MutableComponent text = Component.translatable("imm_ptl.incompatible", mod.modName, mod.modId)
                                    .withStyle(ChatFormatting.RED)
                                    .append(IPMcHelper.getDisableWarningText());
                                
                                if (mod.desc != null) {
                                    text.append(Component.literal(" " + mod.desc + " "));
                                }
                                
                                if (mod.link != null) {
                                    text.append(McHelper.getLinkText(" " + mod.link));
                                }
                                
                                CHelper.printChat(text);
                            }
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
