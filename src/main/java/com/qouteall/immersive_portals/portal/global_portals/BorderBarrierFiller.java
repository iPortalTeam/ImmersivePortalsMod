package com.qouteall.immersive_portals.portal.global_portals;

import com.google.common.base.Supplier;
import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BorderBarrierFiller {
    private static final WeakHashMap<ServerPlayerEntity, Object> warnedPlayers
        = new WeakHashMap<>();
    
    public static void onCommandExecuted(
        ServerPlayerEntity player
    ) {
        ServerWorld world = (ServerWorld) player.world;
        IntBox borderBox = getBorderBox(world);
        if (borderBox == null) {
            player.sendMessage(new TranslatableText("imm_ptl.not_in_wrapping_zone"));
            return;
        }
        
        boolean warned = warnedPlayers.containsKey(player);
        if (!warned) {
            warnedPlayers.put(player, null);
            player.sendMessage(new TranslatableText("imm_ptl.fill_barrier_warning"));
        }
        else {
            warnedPlayers.remove(player);
            
            McHelper.serverLog(player, "Start filling border");
            
            startFillingBorder(world, borderBox, player);
        }
    }
    
    private static IntBox getBorderBox(ServerWorld world) {
        List<WorldWrappingPortal> worldWrappingPortals = McHelper.getGlobalPortals(world).stream().filter(
            p -> p instanceof WorldWrappingPortal
        ).map(
            p -> ((WorldWrappingPortal) p)
        ).collect(Collectors.toList());
        
        if (worldWrappingPortals.size() != 4) {
            return null;
        }
        
        Box floatBox = new Box(
            worldWrappingPortals.get(0).getPos(),
            worldWrappingPortals.get(1).getPos()
        ).union(
            new Box(
                worldWrappingPortals.get(2).getPos(),
                worldWrappingPortals.get(3).getPos()
            )
        );
        
        return new IntBox(
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
        IntBox borderBox,
        ServerPlayerEntity informer
    ) {
        Supplier<IntStream> xStream = () -> IntStream.range(
            borderBox.l.getX(), borderBox.h.getX() + 1
        );
        Supplier<IntStream> zStream = () -> IntStream.range(
            borderBox.l.getZ(), borderBox.h.getZ() + 1
        );
        BlockPos.Mutable temp = new BlockPos.Mutable();
        BlockPos.Mutable temp1 = new BlockPos.Mutable();
        Stream<BlockPos.Mutable> stream = Streams.concat(
            xStream.get().mapToObj(x -> temp.set(x, 0, borderBox.l.getZ())),
            xStream.get().mapToObj(x -> temp.set(x, 0, borderBox.h.getZ())),
            zStream.get().mapToObj(z -> temp.set(borderBox.l.getX(), 0, z)),
            zStream.get().mapToObj(z -> temp.set(borderBox.h.getX(), 0, z))
        );
        
        BlockPos size = borderBox.getSize();
        int totalColumns = size.getX() * 2 + size.getZ() * 2;
        
        McHelper.performSplittedFindingTaskOnServer(
            stream,
            pos -> {
                for (int y = 0; y < 256; y++) {
                    temp1.set(pos.getX(), y, pos.getZ());
                    world.setBlockState(temp1, Blocks.BARRIER.getDefaultState());
                }
                return false;
            },
            columns -> {
                if (McHelper.getServerGameTime() % 4 == 0) {
                    double progress = ((double) columns) / totalColumns;
                    McHelper.serverLog(
                        informer, Integer.toString((int) (progress * 100)) + "%"
                    );
                }
                return true;
            },
            e -> {
                //nothing
                McHelper.serverLog(
                    informer, "Finished"
                );
            },
            () -> {
            
            },
            () -> {
            
            }
        );
    }
}
