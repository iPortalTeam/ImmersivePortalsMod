package qouteall.imm_ptl.core.dimension_sync;

import com.google.common.collect.Streams;
import qouteall.q_misc_util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import qouteall.q_misc_util.MiscHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DimensionTypeSync {
    
    @Environment(EnvType.CLIENT)
    public static Map<RegistryKey<World>, RegistryKey<DimensionType>> clientTypeMap;
    
    @Environment(EnvType.CLIENT)
    private static DynamicRegistryManager currentDimensionTypeTracker;
    
    @Environment(EnvType.CLIENT)
    public static void onGameJoinPacketReceived(DynamicRegistryManager tracker) {
        currentDimensionTypeTracker = tracker;
    }
    
    @Environment(EnvType.CLIENT)
    private static Map<RegistryKey<World>, RegistryKey<DimensionType>> typeMapFromTag(NbtCompound tag) {
        Map<RegistryKey<World>, RegistryKey<DimensionType>> result = new HashMap<>();
        tag.getKeys().forEach(key -> {
            RegistryKey<World> worldKey = DimId.idToKey(key);
            
            String val = tag.getString(key);
            
            RegistryKey<DimensionType> typeKey =
                RegistryKey.of(Registry.DIMENSION_TYPE_KEY, new Identifier(val));
            
            result.put(worldKey, typeKey);
        });
        
        return result;
    }
    
    @Environment(EnvType.CLIENT)
    public static void acceptTypeMapData(NbtCompound tag) {
        clientTypeMap = typeMapFromTag(tag);
        
        Helper.log("Received Dimension Type Sync");
        Helper.log("\n" + Helper.myToString(
            clientTypeMap.entrySet().stream().map(
                e -> e.getKey().getValue().toString() + " -> " + e.getValue().getValue()
            )
        ));
    }
    
    public static NbtCompound createTagFromServerWorldInfo() {
        DynamicRegistryManager registryManager = MiscHelper.getServer().getRegistryManager();
        Registry<DimensionType> dimensionTypes = registryManager.get(Registry.DIMENSION_TYPE_KEY);
        return typeMapToTag(
            Streams.stream(MiscHelper.getServer().getWorlds()).collect(
                Collectors.toMap(
                    World::getRegistryKey,
                    w -> {
                        DimensionType dimensionType = w.getDimension();
                        Identifier id = dimensionTypes.getId(dimensionType);
                        if (id == null) {
                            Helper.err("Missing dim type id for " + w.getRegistryKey());
                            Helper.err("Registered dimension types " +
                                Helper.myToString(dimensionTypes.getIds().stream()));
                            return DimensionType.OVERWORLD_REGISTRY_KEY;
                        }
                        return idToDimType(id);
                    }
                )
            )
        );
    }
    
    public static RegistryKey<DimensionType> idToDimType(Identifier id) {
        return RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id);
    }
    
    private static NbtCompound typeMapToTag(Map<RegistryKey<World>, RegistryKey<DimensionType>> data) {
        NbtCompound tag = new NbtCompound();
        data.forEach((worldKey, typeKey) -> {
            tag.put(worldKey.getValue().toString(), NbtString.of(typeKey.getValue().toString()));
        });
        return tag;
    }
    
    @Environment(EnvType.CLIENT)
    public static RegistryKey<DimensionType> getDimensionTypeKey(RegistryKey<World> worldKey) {
        if (worldKey == World.OVERWORLD) {
            return DimensionType.OVERWORLD_REGISTRY_KEY;
        }
        
        if (worldKey == World.NETHER) {
            return DimensionType.THE_NETHER_REGISTRY_KEY;
        }
        
        if (worldKey == World.END) {
            return DimensionType.THE_END_REGISTRY_KEY;
        }
        
        RegistryKey<DimensionType> obj = clientTypeMap.get(worldKey);
        
        if (obj == null) {
            Helper.err("Missing Dimension Type For " + worldKey);
            return DimensionType.OVERWORLD_REGISTRY_KEY;
        }
        
        return obj;
    }
    
    @Environment(EnvType.CLIENT)
    public static DimensionType getDimensionType(RegistryKey<DimensionType> registryKey) {
        return currentDimensionTypeTracker.get(Registry.DIMENSION_TYPE_KEY).get(registryKey);
    }
    
}
