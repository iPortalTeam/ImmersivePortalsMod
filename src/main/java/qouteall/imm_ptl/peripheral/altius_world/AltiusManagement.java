package qouteall.imm_ptl.peripheral.altius_world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;

import java.util.List;
import java.util.stream.Collectors;

public class AltiusManagement {
    // This is for client dimension stack initialization
    public static AltiusInfo dimensionStackPortalsToGenerate = null;
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(AltiusManagement::serverTick);
    }
    
    private static void serverTick() {
        if (dimensionStackPortalsToGenerate != null) {
            dimensionStackPortalsToGenerate.createPortals();
            dimensionStackPortalsToGenerate = null;
            AltiusGameRule.setIsDimensionStack(true);
        }
    }
    
    public static class RemoteCallables {
        @Environment(EnvType.CLIENT)
        public static void clientOpenScreen(List<String> dimensions) {
            List<RegistryKey<World>> dimensionList =
                dimensions.stream().map(DimId::idToKey).collect(Collectors.toList());
            
            MinecraftClient.getInstance().setScreen(new AltiusScreen(
                null,
                () -> dimensionList,
                altiusInfo -> {
                    if (altiusInfo != null) {
                        McRemoteProcedureCall.tellServerToInvoke(
                            "qouteall.imm_ptl.peripheral.altius_world.AltiusManagement.RemoteCallables.serverSetupDimStack",
                            altiusInfo.toNbt()
                        );
                    }
                    else {
                        McRemoteProcedureCall.tellServerToInvoke(
                            "qouteall.imm_ptl.peripheral.altius_world.AltiusManagement.RemoteCallables.serverRemoveDimStack"
                        );
                    }
                })
            );
        }
        
        public static void serverSetupDimStack(
            ServerPlayerEntity player, NbtCompound infoTag
        ) {
            if (!player.hasPermissionLevel(2)) {
                Helper.err("one player without permission tries to change dimension stack");
                return;
            }
            
            AltiusInfo altiusInfo = AltiusInfo.fromNbt(infoTag);
            
            clearDimStackPortals();
            
            altiusInfo.createPortals();
            
            AltiusGameRule.setIsDimensionStack(true);
            
            player.sendMessage(
                new TranslatableText("imm_ptl.dim_stack_established"),
                false
            );
        }
        
        public static void serverRemoveDimStack(
            ServerPlayerEntity player
        ) {
            if (!player.hasPermissionLevel(2)) {
                Helper.err("one player without permission tries to change dimension stack");
                return;
            }
            
            clearDimStackPortals();
            
            AltiusGameRule.setIsDimensionStack(false);
            
            player.sendMessage(
                new TranslatableText("imm_ptl.dim_stack_removed"),
                false
            );
        }
    }
    
    private static void clearDimStackPortals() {
        for (ServerWorld world : MiscHelper.getServer().getWorlds()) {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            gps.data.removeIf(p -> p instanceof VerticalConnectingPortal);
            gps.bedrockReplacement = null;
            gps.onDataChanged();
        }
    }
}
