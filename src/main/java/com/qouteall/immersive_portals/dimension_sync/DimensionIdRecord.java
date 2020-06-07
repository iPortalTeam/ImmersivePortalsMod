package com.qouteall.immersive_portals.dimension_sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DimensionIdRecord {
    
    public static DimensionIdRecord clientRecord;
    
    public static DimensionIdRecord serverRecord;
    
    private final BiMap<RegistryKey<World>, Integer> idMap;
    private final BiMap<Integer, RegistryKey<World>> inverseMap;
    
    public DimensionIdRecord(BiMap<RegistryKey<World>, Integer> data) {
        idMap = data;
        inverseMap = data.inverse();
    }
    
    public RegistryKey<World> getDim(int integerId) {
        RegistryKey<World> result = inverseMap.get(integerId);
        if (result == null) {
            throw new RuntimeException(
                "Missing Dimension Integer Id " + integerId
            );
        }
        return result;
    }
    
    public int getIntId(RegistryKey<World> dim) {
        Integer result = idMap.get(dim);
        if (result == null) {
            throw new RuntimeException(
                "Missing Dimension Integer Id " + dim
            );
        }
        return result;
    }
    
    @Override
    public String toString() {
        return idMap.entrySet().stream().map(
            e -> e.getKey().toString() + "->" + e.getValue()
        ).collect(Collectors.joining("\n"));
    }
    
    //return null for failed
    private static DimensionIdRecord readIPDimensionRegistry() {
        File dataFile = getIPDimIdFile();
        
        if (!dataFile.exists()) {
            Helper.log("Immersive Portals' Dimension Id Record File Does Not Exist");
            return null;
        }
        
        try {
            FileInputStream fileInputStream = new FileInputStream(dataFile);
            CompoundTag tag = NbtIo.readCompressed(fileInputStream);
            fileInputStream.close();
            
            return tagToRecord(tag);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static File getIPDimIdFile() {
        Path saveDir = McHelper.getServer().session.directory;
        return new File(new File(saveDir.toFile(), "data"), "imm_ptl_dim_reg.dat");
    }
    
    public static DimensionIdRecord tagToRecord(CompoundTag tag) {
        CompoundTag intids = tag.getCompound("intids");
        
        if (intids == null) {
            return null;
        }
        
        HashBiMap<RegistryKey<World>, Integer> bimap = HashBiMap.create();
        
        intids.getKeys().forEach(dim -> {
            if (intids.contains(dim)) {
                int intid = intids.getInt(dim);
                bimap.put(DimId.idToKey(dim), intid);
            }
        });
        
        return new DimensionIdRecord(bimap);
    }
    
    public static CompoundTag recordToTag(DimensionIdRecord record) {
        CompoundTag intids = new CompoundTag();
        record.idMap.forEach((key, intid) -> {
            intids.put(key.getValue().toString(), IntTag.of(intid));
        });
        
        CompoundTag result = new CompoundTag();
        result.put("intids", intids);
        return result;
    }
    
    public static void onReadFabricRegistryServerSide(CompoundTag fabricRegistryRecord) {
        DimensionIdRecord ipRecord = readIPDimensionRegistry();
        
        if (ipRecord != null) {
            Helper.log("Successfully retrieved Immersive Portals' dimension id info");
            serverRecord = ipRecord;
            return;
        }
        
        DimensionIdRecord fabricRecord = readIdsFromFabricRegistryRecord(fabricRegistryRecord);
        
        if (fabricRecord != null) {
            Helper.log("Retrieved Fabric's dimension id info although IP's is missing");
            serverRecord = fabricRecord;
            return;
        }
        
        Helper.err("Cannot retrieve dimension integer id info. The world may get corrupted!!!");
        serverRecord = null;
    }
    
    private static void createServerIdRecordIfMissing() {
        if (serverRecord == null) {
            Helper.err("Dimension Id Record is Missing");
            
            Set<RegistryKey<World>> keys = McHelper.getServer().getWorldRegistryKeys();
            
            HashBiMap<RegistryKey<World>, Integer> bimap = HashBiMap.create();
            
            bimap.put(World.OVERWORLD, 0);
            bimap.put(World.NETHER, -1);
            bimap.put(World.END, 1);
            
            List<RegistryKey<World>> keysList = new ArrayList<>(keys);
            keysList.sort(Comparator.comparing(RegistryKey::toString));
            
            Helper.log("Sorted Dimension Key List:");
            Helper.log(Helper.myToString(keysList.stream()));
            
            keysList.forEach(dim -> {
                if (!bimap.containsKey(dim)) {
                    int newid = bimap.values().stream().mapToInt(i -> i).max().orElse(1) + 1;
                    bimap.put(dim, newid);
                }
            });
            
            serverRecord = new DimensionIdRecord(bimap);
            
            Helper.log("Created Dimension Id Record");
        }
    }
    
    public static void saveServerSideDimInfo() {
        createServerIdRecordIfMissing();
        
        File file = getIPDimIdFile();
        
        try {
            FileOutputStream fileInputStream = new FileOutputStream(file);
            
            CompoundTag tag = recordToTag(serverRecord);
            
            NbtIo.writeCompressed(tag, fileInputStream);
            
            Helper.log("Dimension Id Info Saved to File");
            
            Helper.log("\n" + serverRecord.toString());
        }
        catch (IOException e) {
            throw new RuntimeException(
                "Cannot Save Immersive Portals Dimension Id Info", e
            );
        }
    }
    
    /**
     * {@link RegistrySyncManager#apply(CompoundTag, RemappableRegistry.RemapMode)}
     */
    private static DimensionIdRecord readIdsFromFabricRegistryRecord(CompoundTag fabricRegistryRecord) {
        CompoundTag mainTag = fabricRegistryRecord.getCompound("registries");
        
        if (mainTag == null) {
            Helper.err("Missing 'registries' " + fabricRegistryRecord);
            return null;
        }
        
        CompoundTag dimensionTypeTag = mainTag.getCompound("dimension_type");
        
        if (dimensionTypeTag == null) {
            Helper.err("Missing 'dimension_type' " + fabricRegistryRecord);
            Helper.err("The dimension type id record is already overridden!");
            return null;
        }
        
        HashBiMap<RegistryKey<World>, Integer> bimap = HashBiMap.create();
        
        dimensionTypeTag.getKeys().forEach(dim -> {
            Tag t = dimensionTypeTag.get(dim);
            if (t instanceof IntTag) {
                bimap.put(DimId.idToKey(dim), ((IntTag) t).getInt());
            }
            else {
                Helper.err(String.format(
                    "Non-int tag in fabric registry data %s %s %s", t, dim, fabricRegistryRecord
                ));
            }
        });
        
        return new DimensionIdRecord(bimap);
    }
    
    
}
