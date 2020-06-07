package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;

import java.io.InputStream;
import java.nio.file.Files;

public class O_O {
    public static boolean isReachEntityAttributesPresent;
    
    public static boolean isForge() {
        return false;
    }
    
    @Environment(EnvType.CLIENT)
    public static void onPlayerChangeDimensionClient(
        RegistryKey<World> from, RegistryKey<World> to
    ) {
        RequiemCompat.onPlayerTeleportedClient();
    }
    
    @Environment(EnvType.CLIENT)
    public static void segregateClientEntity(
        ClientWorld fromWorld,
        Entity entity
    ) {
        ((IEClientWorld_MA) fromWorld).removeEntityWhilstMaintainingCapability(entity);
        entity.removed = false;
    }
    
    public static void segregateServerEntity(
        ServerWorld fromWorld,
        Entity entity
    ) {
        fromWorld.removeEntity(entity);
        entity.removed = false;
    }
    
    public static void segregateServerPlayer(
        ServerWorld fromWorld,
        ServerPlayerEntity player
    ) {
        fromWorld.removePlayer(player);
        player.removed = false;
    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayerEntity player,
        DimensionType from,
        DimensionType to
    ) {
        RequiemCompat.onPlayerTeleportedServer(player);
    }
    
    public static void loadConfigFabric() {
        MyConfig myConfig = MyConfig.readConfigFromFile();
        myConfig.onConfigChanged();
        myConfig.saveConfigFile();
    }
    
    public static boolean isObsidian(WorldAccess world, BlockPos obsidianPos) {
        return world.getBlockState(obsidianPos) == Blocks.OBSIDIAN.getDefaultState();
    }
    
    public static void registerDimensionsForge() {
    
    }
    
    public static boolean detectOptiFine() {
        boolean isOptiFabricPresent = FabricLoader.INSTANCE.isModLoaded("optifabric");
        
        if (!isOptiFabricPresent) {
            return false;
        }
        
        try {
            //do not load other optifine classes that loads vanilla classes
            //that would load the class before mixin
            Class.forName("optifine.ZipResourceProvider");
            return true;
        }
        catch (ClassNotFoundException e) {
            Helper.err("OptiFabric is present but OptiFine is not present!!!");
            return false;
        }
    }
    
    public static void postChunkLoadEventForge(WorldChunk chunk) {
    
    }
    
    public static void postChunkUnloadEventForge(WorldChunk chunk) {
    
    }
    
    public static InputStream getLanguageFileStream(String modid) {
        try {
            //noinspection OptionalGetWithoutIsPresent
            return Files.newInputStream(
                net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(modid).get()
                    .getPath("assets/" + modid + "/lang/en_us.json")
            );
        }
        catch (Throwable ugh) {
            throw new RuntimeException(ugh);
        }
    }
    
    public static boolean isDedicatedServer() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }
}
