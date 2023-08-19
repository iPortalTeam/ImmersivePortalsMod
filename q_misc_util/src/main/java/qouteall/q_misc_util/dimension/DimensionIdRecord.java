package qouteall.q_misc_util.dimension;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DimensionIdRecord {
    
    public static DimensionIdRecord clientRecord;
    
    public static DimensionIdRecord serverRecord;
    
    final BiMap<ResourceKey<Level>, Integer> idMap;
    final BiMap<Integer, ResourceKey<Level>> inverseMap;
    
    public DimensionIdRecord(BiMap<ResourceKey<Level>, Integer> data) {
        idMap = data;
        inverseMap = data.inverse();
    }
    
    public ResourceKey<Level> getDim(int integerId) {
        ResourceKey<Level> result = inverseMap.get(integerId);
        if (result == null) {
            throw new RuntimeException(
                "Missing Dimension " + integerId
            );
        }
        return result;
    }
    
    @Nullable
    public ResourceKey<Level> getDimFromIntOptional(int integerId) {
        return inverseMap.get(integerId);
    }
    
    public int getIntId(ResourceKey<Level> dim) {
        Integer result = idMap.get(dim);
        if (result == null) {
            throw new RuntimeException(
                "Missing Dimension " + dim.location()
            );
        }
        return result;
    }
    
    @Override
    public String toString() {
        return idMap.entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getValue()))
            .map(e -> e.getKey().location().toString() + " -> " + e.getValue())
            .collect(Collectors.joining("\n"));
    }
    
    public static DimensionIdRecord tagToRecord(CompoundTag tag) {
        CompoundTag intids = tag.getCompound("intids");
        
        if (intids == null) {
            return null;
        }
        
        HashBiMap<ResourceKey<Level>, Integer> bimap = HashBiMap.create();
        
        intids.getAllKeys().forEach(dim -> {
            if (intids.contains(dim)) {
                int intid = intids.getInt(dim);
                bimap.put(DimId.idToKey(dim), intid);
            }
        });
        
        return new DimensionIdRecord(bimap);
    }
    
    public static CompoundTag recordToTag(
        DimensionIdRecord record, Predicate<ResourceKey<Level>> filter
    ) {
        CompoundTag intids = new CompoundTag();
        record.idMap.forEach((key, intid) -> {
            if (filter.test(key)) {
                intids.put(key.location().toString(), IntTag.valueOf(intid));
            }
        });
        
        CompoundTag result = new CompoundTag();
        result.put("intids", intids);
        return result;
    }
    
    public Set<ResourceKey<Level>> getDimIdSet() {
        return new HashSet<>(idMap.keySet());
    }
    
    
}
