package qouteall.q_misc_util.dimension;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ExtraDimensionStorage {
    
    public static void init() {
        DimensionAPI.serverDimensionsLoadEvent.register(
            ExtraDimensionStorage::loadExtraDimensions
        );
    }
    
    private static void loadExtraDimensions(WorldGenSettings worldGenSettings, RegistryAccess registryAccess) {
        MinecraftServer server = MiscHelper.getServer();
        if (server != null && server.isRunning()) {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
            Registry<LevelStem> dimensionRegistry = worldGenSettings.dimensions();
            
            Path extraStorageFolderPath = getExtraStorageFolderPath();
            File[] subFiles = extraStorageFolderPath.toFile().listFiles();
            if (subFiles != null) {
                for (File nameSpace : subFiles) {
                    if (nameSpace.isDirectory()) {
                        for (File file : nameSpace.listFiles()) {
                            ResourceLocation id = new ResourceLocation(
                                nameSpace.getName(), FilenameUtils.getBaseName(file.getName())
                            );
                            
                            readFile(ops, dimensionRegistry, file, id);
                        }
                    }
                }
            }
        }
    }
    
    private static void readFile(
        RegistryOps<JsonElement> ops, Registry<LevelStem> dimensionRegistry,
        File file, ResourceLocation id
    ) {
        try {
            JsonElement jsonElement;
            try (FileReader fileReader = new FileReader(file)) {
                jsonElement = JsonParser.parseReader(fileReader);
            }
            
            DataResult<Pair<LevelStem, JsonElement>> r =
                LevelStem.CODEC.decode(ops, jsonElement);
            
            Either<
                Pair<LevelStem, JsonElement>,
                DataResult.PartialResult<Pair<LevelStem, JsonElement>>
                > either = r.get();
            
            if (either.left().isPresent()) {
                LevelStem levelStem = either.left().get().getFirst();
                DimensionAPI.addDimension(
                    dimensionRegistry,
                    id,
                    levelStem.typeHolder(),
                    levelStem.generator()
                );
            }
            else {
                Helper.err("Cannot deserialize extra dimension");
                Helper.err(either.right());
            }
        }
        catch (Throwable e) {
            Helper.err("Error loading extra dimension " + id);
            e.printStackTrace();
        }
    }
    
    private static Path getExtraStorageFolderPath() {
        Path savingDirectory = MiscHelper.getWorldSavingDirectory();
        return savingDirectory
            .resolve("q_dimension_configs");
    }
    
    private static File getExtraStorageFile(ResourceLocation location) {
        Path filePath = getExtraStorageFolderPath()
            .resolve(location.getNamespace())
            .resolve(location.getPath() + ".json");
        
        return filePath.toFile();
    }
    
    public static void saveDimensionIntoExtraStorage(ResourceKey<Level> dimension) {
        
        MinecraftServer server = MiscHelper.getServer();
        RegistryAccess.Frozen registryAccess = server.registryAccess();
        ServerLevel world = server.getLevel(dimension);
        Validate.notNull(world);
        
        
        LevelStem levelStem = new LevelStem(
            world.dimensionTypeRegistration(), world.getChunkSource().getGenerator()
        );
        
        File file = getExtraStorageFile(dimension.location());
        
        try {
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            
            try (FileWriter fileWriter = new FileWriter(file)) {
                
                RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
                
                DataResult<JsonElement> r = LevelStem.CODEC.encode(levelStem, ops, new JsonObject());
                Either<JsonElement, DataResult.PartialResult<JsonElement>> either = r.get();
                JsonElement result = either.left().orElse(null);
                if (result != null) {
                    Helper.gson.toJson(result, fileWriter);
                }
                else {
                    Helper.err("Cannot serialize extra dimension");
                    Helper.err(either.right().map(DataResult.PartialResult::toString).orElse(""));
                }
            }
        }
        catch (IOException e) {
            Helper.err("Cannot save extra dimension");
            e.printStackTrace();
        }
    }
    
    public static boolean removeDimensionFromExtraStorage(ResourceKey<Level> dimension) {
        File file = getExtraStorageFile(dimension.location());
        if (file.exists()) {
            file.delete();
            return true;
        }
        return false;
    }
}
