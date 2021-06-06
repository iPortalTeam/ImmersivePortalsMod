package qouteall.imm_ptl.peripheral.altius_world;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import qouteall.imm_ptl.peripheral.ducks.IECreateWorldScreen;
import qouteall.q_misc_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.dynamic.RegistryReadingOps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class WorldCreationDimensionHelper {
    
    public static ResourceManager fetchResourceManager(
        ResourcePackManager resourcePackManager,
        DataPackSettings dataPackSettings,
        DynamicRegistryManager.Impl dynamicRegistryManager
    ) {
        final MinecraftClient client = MinecraftClient.getInstance();
        
        Helper.log("Getting Dimension List");
        
        DataPackSettings dataPackSettings2 = MinecraftServer.loadDataPacks(
            resourcePackManager, dataPackSettings, true
        );
        CompletableFuture<ServerResourceManager> completableFuture =
            ServerResourceManager.reload(
                resourcePackManager.createResourcePacks(),
                dynamicRegistryManager,
                CommandManager.RegistrationEnvironment.INTEGRATED,
                2, Util.getMainWorkerExecutor(), client
            );
        
        client.runTasks(completableFuture::isDone);
        ServerResourceManager serverResourceManager = null;
        try {
            serverResourceManager = (ServerResourceManager) completableFuture.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        return serverResourceManager.getResourceManager();
    }
    
    public static GeneratorOptions getPopulatedGeneratorOptions(
        DynamicRegistryManager.Impl registryTracker, ResourceManager resourceManager,
        GeneratorOptions generatorOptions
    ) {
        RegistryReadingOps<JsonElement> registryReadingOps =
            RegistryReadingOps.of(JsonOps.INSTANCE, registryTracker);
        RegistryOps<JsonElement> registryOps =
            RegistryOps.of(JsonOps.INSTANCE, (ResourceManager) resourceManager, registryTracker);
        
        DynamicRegistryManager.load(registryTracker, registryOps);
        
        DataResult<GeneratorOptions> dataResult =
            GeneratorOptions.CODEC.encodeStart(registryReadingOps, generatorOptions)
                .setLifecycle(Lifecycle.stable())
                .flatMap((jsonElement) -> {
                    return GeneratorOptions.CODEC.parse(registryOps, jsonElement);
                });
        
        GeneratorOptions result = (GeneratorOptions) dataResult.resultOrPartial(
            Util.addPrefix(
                "Error reading worldgen settings after loading data packs: ",
                Helper::log
            )
        ).orElse(generatorOptions);
        
        return result;
        
    }
    
    public static GeneratorOptions populateGeneratorOptions(
        CreateWorldScreen createWorldScreen, GeneratorOptions rawGeneratorOptions,
        DynamicRegistryManager.Impl registryManager
    ) {
        IECreateWorldScreen ieCreateWorldScreen = (IECreateWorldScreen) createWorldScreen;
        
        ResourceManager resourceManager = fetchResourceManager(
            ieCreateWorldScreen.portal_getResourcePackManager(),
            ieCreateWorldScreen.portal_getDataPackSettings(),
            registryManager
        );
        
        GeneratorOptions populatedGeneratorOptions = getPopulatedGeneratorOptions(
            registryManager, resourceManager, rawGeneratorOptions
        );
        return populatedGeneratorOptions;
    }
}
