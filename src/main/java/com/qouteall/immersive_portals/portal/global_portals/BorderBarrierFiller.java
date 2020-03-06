package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

public class BorderBarrierFiller {
    private static final WeakHashMap<ServerPlayerEntity, Object> warnedPlayers
        = new WeakHashMap<>();
    
    public static void onCommandExecuted(
        ServerPlayerEntity player
    ) {
        ServerWorld world = (ServerWorld) player.world;
        IntegerAABBInclusive borderBox = getBorderBox(world);
        if (borderBox == null) {
            McHelper.serverLog(
                player,
                "There is no world wrapping portal in this dimension"
            );
            return;
        }
        
        boolean warned = warnedPlayers.containsKey(player);
        if (!warned) {
            warnedPlayers.put(player, null);
            McHelper.serverLog(
                player,
                "Warning! It will fill the outer layer of the border with barrier blocks.\n" +
                    "This operation cannot be undone. You should backup the world before doing that.\n" +
                    "Invoke this command again to precede."
            );
        }
        else {
            warnedPlayers.remove(player);
            
            McHelper.serverLog(player, "Start filling border");
            
            startFillingBorder(world, borderBox);
        }
    }
    
    private static IntegerAABBInclusive getBorderBox(ServerWorld world) {
        List<BorderPortal> borderPortals = McHelper.getGlobalPortals(world).stream().filter(
            p -> p instanceof BorderPortal
        ).map(
            p -> ((BorderPortal) p)
        ).collect(Collectors.toList());
        
        if (borderPortals.size() != 4) {
            return null;
        }
        
        Box floatBox = new Box(
            borderPortals.get(0).getPos(),
            borderPortals.get(1).getPos()
        ).union(
            new Box(
                borderPortals.get(2).getPos(),
                borderPortals.get(3).getPos()
            )
        );
        
        return new IntegerAABBInclusive(
            new BlockPos(
                floatBox.x1 - 1, -1, floatBox.z1 - 1
            ),
            new BlockPos(
                floatBox.x2, 256, floatBox.z2
            )
        );
    }
    
    private static void startFillingBorder(
        ServerWorld world,
        IntegerAABBInclusive borderBox
    ) {
        //TODO finish it
    }
}
