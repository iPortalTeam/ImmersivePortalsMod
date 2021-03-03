package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.api.PortalAPI;
import com.qouteall.immersive_portals.chunk_loading.ChunkLoader;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.WeakHashMap;

public class ModEntry implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ModMain.init();
        RequiemCompat.init();
        
        MyRegistry.registerEntitiesFabric();
        
        MyRegistry.registerMyDimensionsFabric();
        
        MyRegistry.registerBlocksFabric();
        
        MyRegistry.registerChunkGenerators();
        
        O_O.isReachEntityAttributesPresent =
            FabricLoader.getInstance().isModLoaded("reach-entity-attributes");
        if (O_O.isReachEntityAttributesPresent) {
            Helper.log("Reach entity attributes mod is present");
        }
        else {
            Helper.log("Reach entity attributes mod is not present");
        }
        
        PehkuiInterface.isPehkuiPresent =
            FabricLoader.getInstance().isModLoaded("pehkui");
        if (PehkuiInterface.isPehkuiPresent) {
            PehkuiInterfaceInitializer.init();
            Helper.log("Pehkui is present");
        }
        else {
            Helper.log("Pehkui is not present");
        }
        
        test();
    }
    
    private static final WeakHashMap<ServerPlayerEntity, ChunkLoader> chunkLoaderMap =
        new WeakHashMap<>();
    
    private static void test() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                if (!chunkLoaderMap.containsKey(player)) {
                    ChunkLoader chunkLoader = new ChunkLoader(
                        new DimensionalChunkPos(World.END, 100, 100), 2
                    );
                    chunkLoaderMap.put(player, chunkLoader);
                    PortalAPI.addChunkLoaderForPlayer(player, chunkLoader);
                }
            }
        });
    }
    
}
