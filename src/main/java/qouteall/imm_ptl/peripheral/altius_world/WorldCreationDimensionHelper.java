package qouteall.imm_ptl.peripheral.altius_world;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import qouteall.q_misc_util.Helper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class WorldCreationDimensionHelper {
    
    public static ResourceManager fetchResourceManager(
        PackRepository resourcePackManager,
        DataPackConfig dataPackSettings,
        RegistryAccess.RegistryHolder dynamicRegistryManager
    ) {
        final Minecraft client = Minecraft.getInstance();
        
        Helper.log("Getting Dimension List");
        
        DataPackConfig dataPackSettings2 = MinecraftServer.configurePackRepository(
            resourcePackManager, dataPackSettings, true
        );
        CompletableFuture<ServerResources> completableFuture =
            ServerResources.loadResources(
                resourcePackManager.openAllSelected(),
                dynamicRegistryManager,
                Commands.CommandSelection.INTEGRATED,
                2, Util.backgroundExecutor(), client
            );
        
        client.managedBlock(completableFuture::isDone);
        ServerResources serverResourceManager = null;
        try {
            serverResourceManager = (ServerResources) completableFuture.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        return serverResourceManager.getResourceManager();
    }
    
    public static WorldGenSettings getPopulatedGeneratorOptions(
        RegistryAccess.RegistryHolder registryTracker, ResourceManager resourceManager,
        WorldGenSettings generatorOptions
    ) {
        RegistryWriteOps<JsonElement> registryReadingOps =
            RegistryWriteOps.create(JsonOps.INSTANCE, registryTracker);
        RegistryReadOps<JsonElement> registryOps =
            RegistryReadOps.createAndLoad(JsonOps.INSTANCE, (ResourceManager) resourceManager, registryTracker);
        
        RegistryAccess.load(registryTracker, registryOps);
        
        DataResult<WorldGenSettings> dataResult =
            WorldGenSettings.CODEC.encodeStart(registryReadingOps, generatorOptions)
                .setLifecycle(Lifecycle.stable())
                .flatMap((jsonElement) -> {
                    return WorldGenSettings.CODEC.parse(registryOps, jsonElement);
                });
        
        WorldGenSettings result = (WorldGenSettings) dataResult.resultOrPartial(
            Util.prefix(
                "Error reading worldgen settings after loading data packs: ",
                Helper::log
            )
        ).orElse(generatorOptions);
        
        return result;
        
    }

//    public static GeneratorOptions populateGeneratorOptions(
//        CreateWorldScreen createWorldScreen, GeneratorOptions rawGeneratorOptions,
//        DynamicRegistryManager.Impl registryManager
//    ) {
//        IECreateWorldScreen ieCreateWorldScreen = (IECreateWorldScreen) createWorldScreen;
//
//        ResourcePackManager resourcePackManager = ieCreateWorldScreen.portal_getResourcePackManager();
//        DataPackSettings dataPackSettings = ieCreateWorldScreen.portal_getDataPackSettings();
//        return populateGeneratorOptions1(rawGeneratorOptions, registryManager, resourcePackManager, dataPackSettings);
//    }
    
    public static WorldGenSettings populateGeneratorOptions1(
        WorldGenSettings rawGeneratorOptions,
        RegistryAccess.RegistryHolder registryManager,
        PackRepository resourcePackManager,
        DataPackConfig dataPackSettings
    ) {
        ResourceManager resourceManager = fetchResourceManager(
            resourcePackManager,
            dataPackSettings,
            registryManager
        );
        
        WorldGenSettings populatedGeneratorOptions = getPopulatedGeneratorOptions(
            registryManager, resourceManager, rawGeneratorOptions
        );
        return populatedGeneratorOptions;
    }
}
