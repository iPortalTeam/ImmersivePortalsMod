package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.alternate_dimension.FormulaGenerator;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.EntitySync;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

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
    public static final RegistryKey<DimensionOptions> alternate5Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("immersive_portals:alternate5")
    );
    public static final RegistryKey<DimensionType> surfaceType = RegistryKey.of(
        Registry.DIMENSION_TYPE_KEY,
        new Identifier("immersive_portals:surface_type")
    );
    
    public static final RegistryKey<World> alternate1 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate1")
    );
    public static final RegistryKey<World> alternate2 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate2")
    );
    public static final RegistryKey<World> alternate3 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate3")
    );
    public static final RegistryKey<World> alternate4 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate4")
    );
    public static final RegistryKey<World> alternate5 = RegistryKey.of(
        Registry.DIMENSION,
        new Identifier("immersive_portals:alternate5")
    );
    
    public static DimensionType surfaceTypeObject;
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    public static boolean isAlternateDimension(World world) {
        return world.getDimension() == surfaceTypeObject;
    }
    
    public static void init() {
        Helper.log("Immersive Portals Mod Initializing");
        
        MyNetwork.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
        
        Global.serverTeleportationManager = new ServerTeleportationManager();
        Global.chunkDataSyncManager = new ChunkDataSyncManager();
        
        NewChunkTrackingGraph.init();
        
        WorldInfoSender.init();
        
        FormulaGenerator.init();
        
        GlobalPortalStorage.init();
        
        EntitySync.init();
        
        CollisionHelper.init();
        
    }
    
}
