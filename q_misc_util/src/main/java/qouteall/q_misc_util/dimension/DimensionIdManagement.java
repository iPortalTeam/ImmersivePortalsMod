package qouteall.q_misc_util.dimension;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// ImmPtl in 1.15 and before store integer dimension id
// It used to be able to upgrade the dimension id by reading the id map inside fabric api
// I removed that because Fabric API changed so the hack broke and very few people use 1.15 now
public class DimensionIdManagement {
    public static void onServerStarted(MinecraftServer server) {
        DimensionIdRecord ipRecord = readIPDimensionRegistry(server);
        if (ipRecord == null) {
            Helper.log("Immersive Portals' dimension id record is missing");
            
            DimensionIdRecord.serverRecord = null;
        }
        else {
            DimensionIdRecord.serverRecord = ipRecord;
            Helper.log("Successfully read IP's dimension id record");
        }
        
        updateAndSaveServerDimIdRecord(server);
    }
    
    public static void updateAndSaveServerDimIdRecord(MinecraftServer server) {
        completeServerIdRecord(server);
        
        try {
            File file = getIPDimIdFile(server);
            
            FileOutputStream fileInputStream = new FileOutputStream(file);
            
            CompoundTag tag = DimensionIdRecord.recordToTag(
                DimensionIdRecord.serverRecord, dim -> true
            );
            
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
    private static DimensionIdRecord readIPDimensionRegistry(MinecraftServer server) {
        File dataFile = getIPDimIdFile(server);
        
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
    
    private static File getIPDimIdFile(MinecraftServer server) {
        Path saveDir = MiscHelper.getWorldSavingDirectory(server);
        return new File(new File(saveDir.toFile(), "data"), "imm_ptl_dim_reg.dat");
        // we don't use the vanilla data storage here because it's maybe not usable early enough
    }
    
    private static void completeServerIdRecord(MinecraftServer server) {
        if (DimensionIdRecord.serverRecord == null) {
            Helper.log("Dimension Id Record is Missing");
            DimensionIdRecord.serverRecord = new DimensionIdRecord(HashBiMap.create());
        }
        
        Helper.log("Start Completing Dimension Id Record");
        Helper.log("Before:\n" + DimensionIdRecord.serverRecord);
        
        Set<ResourceKey<Level>> keys = server.levelKeys();
        
        BiMap<ResourceKey<Level>, Integer> bimap = DimensionIdRecord.serverRecord.idMap;
        
        if (!bimap.containsKey(Level.OVERWORLD)) {
            bimap.put(Level.OVERWORLD, 0);
        }
        if (!bimap.containsKey(Level.NETHER)) {
            bimap.put(Level.NETHER, -1);
        }
        if (!bimap.containsKey(Level.END)) {
            bimap.put(Level.END, 1);
        }
        
        List<ResourceKey<Level>> keysList = new ArrayList<>(keys);
        keysList.sort(Comparator.comparing(ResourceKey::toString));
        
        Helper.log("Server Loaded Dimensions:\n" + Helper.myToString(
            keysList.stream().map(ResourceKey::location)
        ));
        
        keysList.forEach(dim -> {
            if (!bimap.containsKey(dim)) {
                int newid = bimap.values().stream().mapToInt(i -> i).max().orElse(1) + 1;
                bimap.put(dim, newid);
            }
        });
        
        Helper.log("After:\n" + DimensionIdRecord.serverRecord);
    }
}
