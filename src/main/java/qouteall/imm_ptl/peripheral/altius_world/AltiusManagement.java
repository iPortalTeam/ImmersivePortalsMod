package qouteall.imm_ptl.peripheral.altius_world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AltiusManagement {
    // This is for client dimension stack initialization
    public static AltiusInfo dimStackToApply = null;
    public static Map<RegistryKey<World>, BlockState> bedrockReplacementMap = new HashMap<>();
    
    public static void init() {
    
    }
    
    // at that time, only overworld has been created, only overworld data can be read
    // it's going to generate overworld spawn chunks
    // make sure the bedrock replacement map for overworld is initialized in time
    public static void onServerEarlyInit(MinecraftServer server) {
        Map<RegistryKey<World>, BlockState> newMap = new HashMap<>();
        
        if (dimStackToApply != null) {
            for (AltiusEntry entry : dimStackToApply.entries) {
                newMap.put(entry.dimension, AltiusInfo.parseBlockString(entry.bedrockReplacementStr));
            }
        }
        
        bedrockReplacementMap = newMap;
    }
    
    public static void onServerCreatedWorlds(MinecraftServer server) {
        if (dimStackToApply != null) {
            dimStackToApply.apply();
            dimStackToApply = null;
        }
        else {
            
            updateBedrockReplacementFromStorage(server);
            
            GameRules gameRules = server.getGameRules();
            GameRules.BooleanRule o = gameRules.get(AltiusGameRule.dimensionStackKey);
            if (o.get()) {
                // legacy dimension stack
                
                o.set(false, server);
                
                upgradeLegacyDimensionStack(server);
            }
        }
    }
    
    private static void updateBedrockReplacementFromStorage(MinecraftServer server) {
        // do not mutate the old map to avoid data race
        Map<RegistryKey<World>, BlockState> newMap = new HashMap<>();
        for (ServerWorld world : server.getWorlds()) {
            BlockState replacement = GlobalPortalStorage.get(world).bedrockReplacement;
            newMap.put(world.getRegistryKey(), replacement);
            Helper.log(String.format(
                "Bedrock Replacement %s %s",
                world.getRegistryKey().getValue(),
                replacement != null ? Registry.BLOCK.getId(replacement.getBlock()) : "null"
            ));
        }
        bedrockReplacementMap = newMap;
    }
    
    public static void upgradeLegacyDimensionStack(MinecraftServer server) {
        
        for (ServerWorld world : server.getWorlds()) {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            gps.bedrockReplacement = Blocks.OBSIDIAN.getDefaultState();
            gps.onDataChanged();
        }
        
        updateBedrockReplacementFromStorage(server);
        
        Helper.log("Legacy Dimension Stack Upgraded");
    }
    
    public static void replaceBedrock(ServerWorld world, Chunk chunk) {
        if (bedrockReplacementMap == null) {
            Helper.err("Dimension Stack Bedrock Replacement Abnormal");
            return;
        }
        
        BlockState replacement = bedrockReplacementMap.get(world.getRegistryKey());
        
        if (replacement != null) {
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                        mutable.set(x, y, z);
                        BlockState blockState = chunk.getBlockState(mutable);
                        if (blockState.getBlock() == Blocks.BEDROCK) {
                            chunk.setBlockState(
                                mutable,
                                replacement,
                                false
                            );
                        }
                    }
                }
            }
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
            
            altiusInfo.apply();
            
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
            
            player.sendMessage(
                new TranslatableText("imm_ptl.dim_stack_removed"),
                false
            );
        }
    }
    
    private static void clearDimStackPortals() {
        MinecraftServer server = MiscHelper.getServer();
        for (ServerWorld world : server.getWorlds()) {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            gps.data.removeIf(p -> p instanceof VerticalConnectingPortal);
            gps.bedrockReplacement = null;
            gps.onDataChanged();
        }
        
        updateBedrockReplacementFromStorage(server);
    }
}
