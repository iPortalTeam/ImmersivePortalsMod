package qouteall.imm_ptl.core.optifine_compatibility;

import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.OFInterface;
import qouteall.imm_ptl.core.optifine_compatibility.mixin_optifine.IEOFBuiltChunk;
import qouteall.imm_ptl.core.optifine_compatibility.mixin_optifine.IEOFConfig;
import qouteall.imm_ptl.core.optifine_compatibility.mixin_optifine.IEOFVboRegion;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class OFBuiltChunkStorageFix {
    private static Method BuiltChunkStorage_updateVboRegion;
    
    private static Field BuiltChunkStorage_mapVboRegions;
    
    private static Method BuiltChunkStorage_deleteVboRegions;
    
    public static void init() {
        BuiltChunkStorage_updateVboRegion = Helper.noError(() ->
            BuiltChunkStorage.class
                .getDeclaredMethod(
                    "updateVboRegion",
                    ChunkBuilder.BuiltChunk.class
                )
        );
        BuiltChunkStorage_updateVboRegion.setAccessible(true);
        
        BuiltChunkStorage_mapVboRegions = Helper.noError(() ->
            BuiltChunkStorage.class
                .getDeclaredField("mapVboRegions")
        );
        BuiltChunkStorage_mapVboRegions.setAccessible(true);
        
        BuiltChunkStorage_deleteVboRegions = Helper.noError(() ->
            BuiltChunkStorage.class
                .getDeclaredMethod(
                    "deleteVboRegions"
                )
        );
    }
    
    public static void onBuiltChunkCreated(
        BuiltChunkStorage builtChunkStorage,
        ChunkBuilder.BuiltChunk builtChunk
    ) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        if (IEOFConfig.ip_isRenderRegions()) {
            Helper.noError(() ->
                BuiltChunkStorage_updateVboRegion.invoke(builtChunkStorage, builtChunk)
            );
        }
    }
    
    public static void purgeRenderRegions(
        MyBuiltChunkStorage storage
    ) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        MinecraftClient.getInstance().getProfiler().push("ip_purge_optifine_render_regions");
        
        Map<ChunkPos, Object> vboRegionMap =
            (Map<ChunkPos, Object>) Helper.noError(() -> BuiltChunkStorage_mapVboRegions.get(storage));
        
        vboRegionMap.entrySet().removeIf(chunkPosObjectEntry -> {
            ChunkPos key = chunkPosObjectEntry.getKey();// it's the start block pos not chunk pos
            Object regionArray = chunkPosObjectEntry.getValue();
            
            // every render region contains 16 * 16 chunks
            
            int regionChunkX = key.x >> 4;
            int regionChunkZ = key.z >> 4;
            
            if (storage.isRegionActive(
                regionChunkX,
                regionChunkZ,
                regionChunkX + 15,
                regionChunkZ + 15
            )) {
                return false;
            }
            else {
                // needs to be deleted
                Object[] regionArray1 = (Object[]) regionArray;
                for (Object o : regionArray1) {
                    ((IEOFVboRegion) o).ip_deleteGlBuffers();
                }
                
                Helper.log("Purged OptiFine render region " + key);
                
                return true;
            }
        });
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    public static void updateNeighbor(
        MyBuiltChunkStorage storage,
        ChunkBuilder.BuiltChunk[] chunks
    ) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        MinecraftClient.getInstance().getProfiler().push("neighbor");
        
        try {
            for (int l = 0; l < Direction.values().length; ++l) {
                Direction facing = Direction.values()[l];
                for (ChunkBuilder.BuiltChunk renderChunk : chunks) {
                    BlockPos neighborPos = renderChunk.getNeighborPosition(facing);
                    ChunkBuilder.BuiltChunk neighbour =
                        storage.getSectionFromRawArray(neighborPos, chunks);
                    
                    ((IEOFBuiltChunk) renderChunk).ip_setRenderChunkNeighbour(
                        facing, neighbour
                    );
                }
            }
        }
        catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    public static void onBuiltChunkStorageCleanup(BuiltChunkStorage builtChunkStorage) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        Helper.noError(() -> {
            BuiltChunkStorage_deleteVboRegions.invoke(builtChunkStorage);
            return null;
        });
    }
}
