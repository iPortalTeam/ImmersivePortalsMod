package qouteall.imm_ptl.core.compat;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.List;

public class IPModCompatibilityWarning {
    
    // I do not put these mods into "breaks" so that compatibility can be debugged
    
    private static List<ModInfo> incompatibleMods = Lists.newArrayList(
        new ModInfo("dimthread", "Dimensional Threading"),
        new ModInfo("c2me", "C2ME"),
        new ModInfo("replaymod", "Replay Mod"),
        new ModInfo("physicsmod", "Physics Mod"),
        new ModInfo("resolutioncontrol", "Resolution Control"),
        new ModInfo("viafabric-mc118", "ViaFabric for MC 1.18"),
        new ModInfo("mcxr-core", "MCXR"),
        new ModInfo("multiconnect", "MultiConnect"),
        new ModInfo("taterzens", "Taterzens"),
        new ModInfo("requiem", "Requiem"),
        new ModInfo("modern_industrialization", "Modern Industrialization"),
        new ModInfo("canvas", "Canvas"),
        new ModInfo("cardboard", "Cardboard"),
        new ModInfo("altoclef", "Altoclef")
    );
    
    public static record ModInfo(String modId, String modName) {}
    
    public static void initDedicatedServer() {
        for (ModInfo mod : incompatibleMods) {
            if (FabricLoader.getInstance().isModLoaded(mod.modId)) {
                Helper.err(String.format(
                    "WARNING: This mod is incompatible with Immersive Portals: %s(%s)",
                    mod.modName, mod.modId
                ));
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        for (ModInfo mod : incompatibleMods) {
            if (FabricLoader.getInstance().isModLoaded(mod.modId)) {
                IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
                    () -> MinecraftClient.getInstance().world == null,
                    MyTaskList.oneShotTask(() -> {
                        CHelper.printChat(new LiteralText(
                            String.format(
                                "WARNING: Immersive Portals mod is incompatible with mod %s(%s) . Major issues may occur.",
                                mod.modName, mod.modId
                            )
                        ).formatted(Formatting.RED));
                    })
                ));
                
            }
        }
    }
}
