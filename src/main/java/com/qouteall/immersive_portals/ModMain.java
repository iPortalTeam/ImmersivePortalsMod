package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.FormulaGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import com.qouteall.immersive_portals.alternate_dimension.VoidChunkGenerator;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Supplier;

public class ModMain {
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    public static final RegistryKey<DimensionOptions> alternate1Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate1")
    );
    public static final RegistryKey<DimensionOptions> alternate2Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate2")
    );
    public static final RegistryKey<DimensionOptions> alternate3Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate3")
    );
    public static final RegistryKey<DimensionOptions> alternate4Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate4")
    );
    public static final RegistryKey<DimensionType> surfaceType = RegistryKey.of(
        Registry.DIMENSION_TYPE_KEY,
        new Identifier("immersive_portals:surface_type")
    );
    
    public static DimensionType surfaceTypeObject;
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    public static boolean isAlternateDimension(World world) {
        return world.getDimension() == surfaceTypeObject;
    }
    
    public static void init() {
        Helper.log("initializing common");
        
        MyNetwork.init();
        
        Helper.log("Network initialized");
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
        
        Global.serverTeleportationManager = new ServerTeleportationManager();
        Global.chunkDataSyncManager = new ChunkDataSyncManager();
        
        Helper.log("Global objects initialized");
        
        NewChunkTrackingGraph.init();
        
        Helper.log("Chunk tracking graph initialized");
        
        WorldInfoSender.init();
        
        Helper.log("World info sender initialized");
        
        FormulaGenerator.init();
        
        GlobalPortalStorage.init();
        
        Registry.register(
            Registry.CHUNK_GENERATOR,
            new Identifier("immersive_portals:normal_skyland"),
            NormalSkylandGenerator.codec
        );
        
        Registry.register(
            Registry.CHUNK_GENERATOR,
            new Identifier("immersive_portals:chaos_terrain"),
            ErrorTerrainGenerator.codec
        );
        
        Registry.register(
            Registry.CHUNK_GENERATOR,
            new Identifier("immersive_portals:void_generator"),
            VoidChunkGenerator.codec
        );
        
    }
    
}
