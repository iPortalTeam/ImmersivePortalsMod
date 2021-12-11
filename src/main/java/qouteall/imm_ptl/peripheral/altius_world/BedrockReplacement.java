package qouteall.imm_ptl.peripheral.altius_world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.Helper;

import java.util.HashMap;
import java.util.Map;

public class BedrockReplacement {
    
    public static Map<RegistryKey<World>, BlockState> bedrockReplacementMap = new HashMap<>();
    
    public static void onServerEarlyInit(MinecraftServer server) {
        updateBedrockReplacementMap(server);
    }
    
    public static void onServerCreatedWorlds(MinecraftServer server) {
        updateBedrockReplacementMap(server);
        
        GameRules gameRules = server.getGameRules();
        GameRules.BooleanRule o = gameRules.get(AltiusGameRule.dimensionStackKey);
        if (o.get()) {
            // legacy dimension stack
            
            o.set(false, server);
            
            upgradeLegacyDimensionStack(server);
        }
    }
    
    private static void updateBedrockReplacementMap(MinecraftServer server) {
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
        
        updateBedrockReplacementMap(server);
        
        Helper.log("Legacy Dimension Stack Upgraded");
    }
    
    public static void replaceBedrock(ServerWorld world, Chunk chunk) {
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
}
