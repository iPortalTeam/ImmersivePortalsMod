package com.qouteall.immersive_portals.dimension_sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class DimensionIdManagement {
    private static Field fabric_activeTag_field;
    
    public static void onServerStarted() {
        DimensionIdRecord ipRecord = readIPDimensionRegistry();
        if (ipRecord == null) {
            Helper.log("Immersive Portals' dimension id record is missing");
            
            DimensionIdRecord fabricRecord = getFabricRecord();
            
            if (fabricRecord != null) {
                Helper.log("Found Fabric's dimension id record");
                Helper.log("\n" + fabricRecord);
                
                DimensionIdRecord.serverRecord = fabricRecord;
                fabricRecord = null;
            }
            else {
                Helper.log("Cannot retrieve Fabric's dimension id record.");
                Helper.log("If this is not a newly created world," +
                    " existing portal data may be corrupted!!!"
                );
                DimensionIdRecord.serverRecord = null;
            }
        }
        else {
            DimensionIdRecord.serverRecord = ipRecord;
            Helper.log("Successfully read IP's dimension id record");
        }
        
        completeServerIdRecord();
        
        try {
            File file = getIPDimIdFile();
            
            FileOutputStream fileInputStream = new FileOutputStream(file);
            
            CompoundTag tag = DimensionIdRecord.recordToTag(DimensionIdRecord.serverRecord);
            
            NbtIo.writeCompressed(tag, fileInputStream);
            
            Helper.log("Dimension Id Info Saved to File");
        }
        catch (IOException e) {
            throw new RuntimeException(
                "Cannot Save Immersive Portals Dimension Id Info", e
            );
        }
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
            
            return DimensionIdRecord.tagToRecord(tag);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static File getIPDimIdFile() {
        MinecraftServer server = McHelper.getServer();
        Validate.notNull(server);
        Path saveDir = server.session.directory;
        return new File(new File(saveDir.toFile(), "data"), "imm_ptl_dim_reg.dat");
    }
    
    private static void completeServerIdRecord() {
        if (DimensionIdRecord.serverRecord == null) {
            Helper.log("Dimension Id Record is Missing");
            DimensionIdRecord.serverRecord = new DimensionIdRecord(HashBiMap.create());
        }
        
        Helper.log("Start Completing Dimension Id Record");
        Helper.log("Before:\n" + DimensionIdRecord.serverRecord);
        
        Set<RegistryKey<World>> keys = McHelper.getServer().getWorldRegistryKeys();
        
        BiMap<RegistryKey<World>, Integer> bimap = DimensionIdRecord.serverRecord.idMap;
        
        if (!bimap.containsKey(World.OVERWORLD)) {
            bimap.put(World.OVERWORLD, 0);
        }
        if (!bimap.containsKey(World.NETHER)) {
            bimap.put(World.NETHER, -1);
        }
        if (!bimap.containsKey(World.END)) {
            bimap.put(World.END, 1);
        }
        
        List<RegistryKey<World>> keysList = new ArrayList<>(keys);
        keysList.sort(Comparator.comparing(RegistryKey::toString));
        
        Helper.log("Server Loaded Dimensions:\n" + Helper.myToString(
            keysList.stream().map(RegistryKey::getValue)
        ));
        
        keysList.forEach(dim -> {
            if (!bimap.containsKey(dim)) {
                int newid = bimap.values().stream().mapToInt(i -> i).max().orElse(1) + 1;
                bimap.put(dim, newid);
            }
        });
        
        Helper.log("After:\n" + DimensionIdRecord.serverRecord);
    }
    
    /**
     * MixinLevelStorageSession
     */
    @Nullable
    private static DimensionIdRecord getFabricRecord() {
        if (O_O.isForge()) {
            return null;
        }
        
        try {
            if (fabric_activeTag_field == null) {
                fabric_activeTag_field = LevelStorage.Session.class.getDeclaredField("fabric_activeTag");
                fabric_activeTag_field.setAccessible(true);
            }
            
            LevelStorage.Session session = McHelper.getServer().session;
            
            CompoundTag tag = (CompoundTag) fabric_activeTag_field.get(session);
            
            if (tag == null) {
                return null;
            }
            
            return readIdsFromFabricRegistryRecord(tag);
        }
        catch (Throwable e) {
            throw new RuntimeException(
                "Cannot get Fabric registry info", e
            );
        }
    }
    
    /**
     * RegistrySyncManager#apply(CompoundTag, RemappableRegistry.RemapMode)
     */
    private static DimensionIdRecord readIdsFromFabricRegistryRecord(CompoundTag fabricRegistryRecord) {
        CompoundTag dimensionTypeTag = fabricRegistryRecord.getCompound("minecraft:dimension_type");
        
        if (dimensionTypeTag.isEmpty()) {
            Helper.err("Missing 'minecraft:dimension_type' " + fabricRegistryRecord);
            return null;
        }
        
        HashBiMap<RegistryKey<World>, Integer> bimap = HashBiMap.create();
        
        dimensionTypeTag.getKeys().forEach(dim -> {
            Tag t = dimensionTypeTag.get(dim);
            if (t instanceof IntTag) {
                int data = ((IntTag) t).getInt();
                bimap.put(DimId.idToKey(dim), data - 1);
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
