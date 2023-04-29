package qouteall.imm_ptl.core.compat;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.MyTaskList;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class IPModInfoChecking {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
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
        public List<String> incompatibleShaderpacks;
        
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
    public static ImmPtlInfo fetchImmPtlInfoFromInternet() {
        String url = O_O.getImmPtlModInfoUrl();
        
        if (url == null) {
            Helper.log("Not fetching immptl mod info");
            return null;
        }
        
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Immersive Portals mod")
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() != 200) {
                Helper.err("Failed to fetch immptl mod info " + response.statusCode());
                return null;
            }
            
            String jsonStr = response.body();
            ImmPtlInfo immPtlInfo = Helper.gson.fromJson(jsonStr, ImmPtlInfo.class);
            LOGGER.info("ImmPtl mod info fetched");
            return immPtlInfo;
        }
        catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
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
            
            incompatibleShaderpacks = immPtlInfo.incompatibleShaderpacks;
            
            IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
                () -> Minecraft.getInstance().level == null,
                MyTaskList.oneShotTask(() -> {
                    List<Component> texts = new ArrayList<>();
                    
                    if (IPGlobal.enableUpdateNotification) {
                        if (O_O.shouldUpdateImmPtl(immPtlInfo.latestReleaseVersion)) {
                            MutableComponent text1 = Component.translatable(
                                "imm_ptl.new_version_available",
                                immPtlInfo.latestReleaseVersion
                            );
                            text1.append(McHelper.getLinkText(O_O.getModDownloadLink()));
                            
                            text1.append(Component.literal("  "));
                            text1.append(IPMcHelper.getDisableUpdateCheckText());
                            
                            texts.add(text1);
                        }
                    }
                    
                    for (ModIncompatInfo mod : immPtlInfo.severelyIncompatible) {
                        if (mod != null && mod.isModLoadedWithinVersion()) {
                            MutableComponent text1;
                            if (mod.startVersion != null || mod.endVersion != null) {
                                text1 = Component.translatable(
                                    "imm_ptl.severely_incompatible_within_version",
                                    mod.modName, mod.modId,
                                    mod.getVersionRangeStr()
                                ).withStyle(ChatFormatting.RED);
                            }
                            else {
                                text1 = Component.translatable("imm_ptl.severely_incompatible", mod.modName, mod.modId)
                                    .withStyle(ChatFormatting.RED);
                            }
                            
                            if (mod.desc != null) {
                                text1.append(Component.literal(" " + mod.desc + " "));
                            }
                            
                            if (mod.link != null) {
                                text1.append(Component.literal(" "));
                                text1.append(McHelper.getLinkText(mod.link));
                            }
                            
                            texts.add(text1);
                        }
                    }
                    
                    for (ModIncompatInfo mod : immPtlInfo.incompatible) {
                        if (mod != null && mod.isModLoadedWithinVersion()) {
                            if (IPConfig.getConfig().shouldDisplayWarning(mod.modId)) {
                                MutableComponent text1 = Component.translatable("imm_ptl.incompatible", mod.modName, mod.modId)
                                    .withStyle(ChatFormatting.RED)
                                    .append(IPMcHelper.getDisableWarningText(mod.modId));
                                
                                if (mod.desc != null) {
                                    text1.append(Component.literal(" " + mod.desc + " "));
                                }
                                
                                if (mod.link != null) {
                                    text1.append(McHelper.getLinkText(" " + mod.link));
                                }
                                
                                texts.add(text1);
                            }
                        }
                    }
                    
                    for (Component text : texts) {
                        CHelper.printChat(text);
                    }
                })
            ));
        });
        
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPConfig.getConfig().shouldDisplayWarning("many_mods") && !FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    List<ModContainer> topLevelMods = FabricLoader.getInstance().getAllMods().stream()
                        .filter(modContainer -> modContainer.getContainingMod().isEmpty()).toList();
                    
                    if (topLevelMods.size() > 20) {
                        CHelper.printChat(Component.literal(
                            "[Immersive Portals] WARNING: You are using many mods. It's likely that one of them has compatibility issues with Immersive Portals. " +
                                "If you are sure that there is no compatibility issue, disable this warning."
                        ).withStyle(ChatFormatting.RED).append(IPMcHelper.getDisableWarningText("many_mods")));
                    }
                }
            })
        ));
        
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
            
            MiscHelper.getServer().execute(() -> {
                if (IPGlobal.enableUpdateNotification) {
                    if (O_O.shouldUpdateImmPtl(immPtlInfo.latestReleaseVersion)) {
                        LOGGER.info("[Immersive Portals] A new version is available. It is recommended to update to " + immPtlInfo.latestReleaseVersion);
                    }
                }
                
                for (ModIncompatInfo mod : immPtlInfo.severelyIncompatible) {
                    if (mod != null && mod.isModLoadedWithinVersion()) {
                        MutableComponent text1;
                        if (mod.startVersion != null || mod.endVersion != null) {
                            text1 = Component.translatable(
                                "imm_ptl.severely_incompatible_within_version",
                                mod.modName, mod.modId,
                                mod.getVersionRangeStr()
                            ).withStyle(ChatFormatting.RED);
                            LOGGER.error(
                                "[Immersive Portals] Detected an incompatible mod: {} {} version range: {} description: {} link: {}",
                                mod.modName, mod.modId, mod.getVersionRangeStr(), mod.desc, mod.link
                            );
                        }
                        else {
                            text1 = Component.translatable("imm_ptl.severely_incompatible", mod.modName, mod.modId)
                                .withStyle(ChatFormatting.RED);
                            LOGGER.error(
                                "[Immersive Portals] Detected an incompatible mod: {} {} description: {} link: {}",
                                mod.modName, mod.modId, mod.desc, mod.link
                            );
                        }
                        
                        if (mod.desc != null) {
                            text1.append(Component.literal(" " + mod.desc + " "));
                        }
                        
                        if (mod.link != null) {
                            text1.append(Component.literal(" "));
                            text1.append(McHelper.getLinkText(mod.link));
                        }
                        
                        McHelper.sendMessageToFirstLoggedPlayer(
                            Component.translatable("imm_ptl.message_from_server")
                                .append(text1)
                        );
                    }
                }
                
                for (ModIncompatInfo mod : immPtlInfo.incompatible) {
                    if (mod != null && mod.isModLoadedWithinVersion()) {
                        if (IPConfig.getConfig().shouldDisplayWarning(mod.modId)) {
                            MutableComponent text1 = Component.translatable("imm_ptl.incompatible", mod.modName, mod.modId)
                                .withStyle(ChatFormatting.RED);
                            
                            if (mod.desc != null) {
                                text1.append(Component.literal(" " + mod.desc + " "));
                            }
                            
                            if (mod.link != null) {
                                text1.append(McHelper.getLinkText(" " + mod.link));
                            }
                            
                            LOGGER.error("[Immersive Portals] Detected a mildly incompatible mod: {} {} description: {} link: {}",
                                mod.modName, mod.modId, mod.desc, mod.link
                            );
                            McHelper.sendMessageToFirstLoggedPlayer(
                                Component.translatable("imm_ptl.message_from_server")
                                    .append(text1)
                            );
                        }
                    }
                }
            });
        });
    }
    
    @Nullable
    private static List<String> incompatibleShaderpacks;
    @Nullable
    private static String lastShaderpackName;
    
    public static void checkShaderpack() {
        if (!IPConfig.getConfig().shaderpackWarning) {
            return;
        }
        
        String shaderpackName = IrisInterface.invoker.getShaderpackName();
        if (!Objects.equals(lastShaderpackName, shaderpackName)) {
            lastShaderpackName = shaderpackName;
            
            if (shaderpackName != null) {
                if (incompatibleShaderpacks != null) {
                    boolean incompatible = incompatibleShaderpacks.stream().anyMatch(
                        n -> shaderpackName.toLowerCase().contains(n.toLowerCase())
                    );
                    if (incompatible) {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.incompatible_shaderpack")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }
        }
    }
}
