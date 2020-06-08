package com.qouteall.immersive_portals.portal.global_portals;

import com.google.common.base.Supplier;
import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Blocks;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BorderBarrierFiller {
    private static final WeakHashMap<ServerPlayerEntity, Object> warnedPlayers
        = new WeakHashMap<>();
    
    public static void onCommandExecuted(
        ServerPlayerEntity player
    ) {
        ServerWorld world = (ServerWorld) player.world;
        Vec3d playerPos = player.getPos();
        
        List<WorldWrappingPortal.WrappingZone> wrappingZones =
            WorldWrappingPortal.getWrappingZones(world);
        
        WorldWrappingPortal.WrappingZone zone = wrappingZones.stream().filter(
            wrappingZone -> wrappingZone.getArea().contains(playerPos)
        ).findFirst().orElse(null);
        
        if (zone == null) {
            player.sendMessage(new TranslatableText("imm_ptl.clear_border_warning"), false);
            return;
        }
        
        doInvoke(player, world, zone);
    }
    
    public static void onCommandExecuted(
        ServerPlayerEntity player,
        int zoneId
    ) {
        ServerWorld world = (ServerWorld) player.world;
        
        List<WorldWrappingPortal.WrappingZone> wrappingZones =
            WorldWrappingPortal.getWrappingZones(world);
        
        WorldWrappingPortal.WrappingZone zone = wrappingZones.stream().filter(
            wrappingZone -> wrappingZone.id == zoneId
        ).findFirst().orElse(null);
        
        if (zone == null) {
            player.sendMessage(new TranslatableText("imm_ptl.cannot_find_zone"), false);
            return;
        }
        
        doInvoke(player, world, zone);
    }
    
    private static void doInvoke(
        ServerPlayerEntity player,
        ServerWorld world,
        WorldWrappingPortal.WrappingZone zone
    ) {
        IntBox borderBox = zone.getBorderBox();
        
        boolean warned = warnedPlayers.containsKey(player);
        if (!warned) {
            warnedPlayers.put(player, null);
            player.sendMessage(new TranslatableText("imm_ptl.clear_border_warning"),
                false);
        }
        else {
            warnedPlayers.remove(player);
            
            player.sendMessage(new TranslatableText("imm_ptl.start_clearing_border"),
                false);
            
            startFillingBorder(world, borderBox, l -> player.sendMessage(l, false));
        }
    }
    
    private static void startFillingBorder(
        ServerWorld world,
        IntBox borderBox,
        Consumer<Text> informer
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
                    world.setBlockState(temp1, Blocks.AIR.getDefaultState());
                }
                return false;
            },
            columns -> {
                double progress = ((double) columns) / totalColumns;
                informer.accept(new LiteralText(
                    Integer.toString((int) (progress * 100)) + "%"
                ));
                return true;
            },
            e -> {
                //nothing
            },
            () -> {
                informer.accept(new TranslatableText("imm_ptl.finished"));
            },
            () -> {
            
            }
        );
    }
}
