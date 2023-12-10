package qouteall.q_misc_util.dimension;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DimIntIdMap {
    
    public static final int MISSING_ID = Integer.MIN_VALUE;
    
    final Object2IntOpenHashMap<ResourceKey<Level>> toIntegerId;
    final Int2ObjectOpenHashMap<ResourceKey<Level>> fromIntegerId;
    private int maxId;
    
    public DimIntIdMap(
        Object2IntOpenHashMap<ResourceKey<Level>> toIntegerId,
        Int2ObjectOpenHashMap<ResourceKey<Level>> fromIntegerId
    ) {
        this.toIntegerId = toIntegerId;
        this.fromIntegerId = fromIntegerId;
        toIntegerId.defaultReturnValue(MISSING_ID);
        maxId = toIntegerId.values().intStream().max().orElse(0);
    }
    
    public DimIntIdMap() {
        this(
            new Object2IntOpenHashMap<>(),
            new Int2ObjectOpenHashMap<>()
        );
    }
    
    public ResourceKey<Level> fromIntegerId(int integerId) {
        ResourceKey<Level> result = fromIntegerId.get(integerId);
        if (result == null) {
            throw new RuntimeException(
                "Missing Dimension " + integerId
            );
        }
        return result;
    }
    
    @Nullable
    public ResourceKey<Level> fromIntegerIdNullable(int integerId) {
        return fromIntegerId.get(integerId);
    }
    
    public int toIntegerId(ResourceKey<Level> dim) {
        int result = toIntegerId.getInt(dim);
        if (result == MISSING_ID) {
            throw new RuntimeException(
                "Missing Dimension " + dim.location()
            );
        }
        return result;
    }
    
    public void add(ResourceKey<Level> dimId, int intId) {
        if (toIntegerId.containsKey(dimId)) {
            throw new RuntimeException(
                "Dimension Id Record already contains " + dimId.location() + " " + this
            );
        }
        if (fromIntegerId.containsKey(intId)) {
            throw new RuntimeException(
                "Dimension Id Record already contains " + intId + " " + this
            );
        }
        toIntegerId.put(dimId, intId);
        fromIntegerId.put(intId, dimId);
        maxId = Math.max(maxId, intId);
    }
    
    public boolean remove(ResourceKey<Level> dimId) {
        int intId = toIntegerId.removeInt(dimId);
        if (intId == MISSING_ID) {
            return false;
        }
        fromIntegerId.remove(intId);
        return true;
    }
    
    public boolean removeUnused(Set<ResourceKey<Level>> currentDimIds) {
        boolean changed = false;
        for (ResourceKey<Level> dimId : toIntegerId.keySet()) {
            if (!currentDimIds.contains(dimId)) {
                remove(dimId);
                changed = true;
            }
        }
        return changed;
    }
    
    public boolean containsDimId(ResourceKey<Level> dimId) {
        return toIntegerId.containsKey(dimId);
    }
    
    public boolean containsIntId(int intId) {
        return fromIntegerId.containsKey(intId);
    }
    
    public static DimIntIdMap tagToRecord(CompoundTag tag) {
        CompoundTag intids = tag.getCompound("intids");
        
        Object2IntOpenHashMap<ResourceKey<Level>> toIntegerId = new Object2IntOpenHashMap<>();
        Int2ObjectOpenHashMap<ResourceKey<Level>> fromIntegerId = new Int2ObjectOpenHashMap<>();
        
        intids.getAllKeys().forEach(dim -> {
            if (intids.contains(dim)) {
                int intid = intids.getInt(dim);
                ResourceKey<Level> dimId = Helper.dimIdToKey(dim);
                toIntegerId.put(dimId, intid);
                fromIntegerId.put(intid, dimId);
            }
        });
        
        return new DimIntIdMap(
            toIntegerId, fromIntegerId
        );
    }
    
    public static CompoundTag recordToTag(
        DimIntIdMap record, Predicate<ResourceKey<Level>> filter
    ) {
        CompoundTag intids = new CompoundTag();
        record.toIntegerId.forEach((key, intid) -> {
            if (filter.test(key)) {
                intids.put(key.location().toString(), IntTag.valueOf(intid));
            }
        });
        
        CompoundTag result = new CompoundTag();
        result.put("intids", intids);
        return result;
    }
    
    public Set<ResourceKey<Level>> getDimIdSet() {
        return Collections.unmodifiableSet(toIntegerId.keySet());
    }
    
    public int getNextIntegerId() {
        return maxId + 1;
    }
    
    @Override
    public String toString() {
        return toIntegerId.object2IntEntrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getIntValue()))
            .map(e -> e.getKey().location().toString() + " -> " + e.getIntValue())
            .collect(Collectors.joining("\n"));
    }
}
