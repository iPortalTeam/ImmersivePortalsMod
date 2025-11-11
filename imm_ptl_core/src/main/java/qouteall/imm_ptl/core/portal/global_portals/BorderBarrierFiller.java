package qouteall.imm_ptl.core.portal.global_portals;

import com.google.common.base.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BorderBarrierFiller {
    private static final WeakHashMap<ServerPlayer, Object> warnedPlayers
        = new WeakHashMap<>();

    public static void onCommandExecuted(
        ServerPlayer player
    ) {
        ServerLevel world = (ServerLevel) player.level();
        Vec3 playerPos = player.position();

        List<WorldWrappingPortal.WrappingZone> wrappingZones =
            WorldWrappingPortal.getWrappingZones(world);

        WorldWrappingPortal.WrappingZone zone = wrappingZones.stream().filter(
            wrappingZone -> wrappingZone.getArea().contains(playerPos)
        ).findFirst().orElse(null);

        if (zone == null) {
            player.displayClientMessage(Component.translatable("imm_ptl.cannot_find_zone"), false);
            return;
        }

        doInvoke(player, world, zone);
    }

    public static void onCommandExecuted(
        ServerPlayer player,
        int zoneId
    ) {
        ServerLevel world = (ServerLevel) player.level();

        List<WorldWrappingPortal.WrappingZone> wrappingZones =
            WorldWrappingPortal.getWrappingZones(world);

        WorldWrappingPortal.WrappingZone zone = wrappingZones.stream().filter(
            wrappingZone -> wrappingZone.id == zoneId
        ).findFirst().orElse(null);

        if (zone == null) {
            player.displayClientMessage(Component.translatable("imm_ptl.cannot_find_zone"), false);
            return;
        }

        doInvoke(player, world, zone);
    }

    private static void doInvoke(
        ServerPlayer player,
        ServerLevel world,
        WorldWrappingPortal.WrappingZone zone
    ) {
        IntBox borderBox = zone.getBorderBox();

        boolean warned = warnedPlayers.containsKey(player);
        if (!warned) {
            warnedPlayers.put(player, null);

            BlockPos size = borderBox.getSize();
            int totalColumns = size.getX() * 2 + size.getZ() * 2;

            // according to my test 80000 columns increase world saving by 465 MB
            double sizeEstimationGB = (totalColumns / 80000.0) * 0.5;

            player.displayClientMessage(
                Component.translatable(
                    "imm_ptl.clear_border_warning",
                    sizeEstimationGB < 0.01 ? 0 : sizeEstimationGB
                ),
                false
            );
        }
        else {
            warnedPlayers.remove(player);

            player.displayClientMessage(
                Component.translatable("imm_ptl.start_clearing_border"),
                false
            );


            startFillingBorder(world, borderBox, l -> player.displayClientMessage(l, false));
        }
    }

    private static void startFillingBorder(
        ServerLevel world,
        IntBox borderBox,
        Consumer<Component> informer
    ) {
        Supplier<IntStream> xStream = () -> IntStream.range(
            borderBox.l.getX(), borderBox.h.getX() + 1
        );
        Supplier<IntStream> zStream = () -> IntStream.range(
            borderBox.l.getZ(), borderBox.h.getZ() + 1
        );

        // Build an explicit list of column positions (avoid reusing a single mutable BlockPos)
        int minX = borderBox.l.getX();
        int maxX = borderBox.h.getX();
        int minZ = borderBox.l.getZ();
        int maxZ = borderBox.h.getZ();

        // Build an indexed list of column positions so we can process them in batches
        List<BlockPos> columnsList = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            columnsList.add(new BlockPos(x, 0, minZ));
            if (minZ != maxZ) columnsList.add(new BlockPos(x, 0, maxZ));
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            columnsList.add(new BlockPos(minX, 0, z));
            if (minX != maxX) columnsList.add(new BlockPos(maxX, 0, z));
        }

        BlockPos size = borderBox.getSize();
        int totalColumns = size.getX() * 2 + size.getZ() * 2;

        int minY = McHelper.getMinY(world);
        int maxYEx = McHelper.getMaxYExclusive(world);

        ThreadedLevelLightEngine lightingProvider = world.getChunkSource().getLightEngine();

        final int columnsTotal = columnsList.size();
        // Server-task based incremental processing to avoid background threads touching chunk internals
        qouteall.q_misc_util.my_util.MyTaskList.MyTask task = new qouteall.q_misc_util.my_util.MyTaskList.MyTask() {
             int index = 0;
            final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            @Override
            public boolean runAndGetIsFinished() {
                int processed = 0;
                while (index < columnsList.size() && processed < 50) {
                    BlockPos col = columnsList.get(index);
                    // ensure this runs on server thread and uses server-safe chunk access
                    ChunkAccess chunk = world.getChunk(col);
                    for (int y = minY; y < maxYEx; y++) {
                        mutable.set(col.getX(), y, col.getZ());
                        chunk.setBlockState(mutable, Blocks.AIR.defaultBlockState(), false);
                        lightingProvider.checkBlock(mutable);
                    }

                    index++;
                    processed++;
                }

                if (McHelper.getServerGameTime() % 20 == 0) {
                    informer.accept(Component.literal(String.format("Progress: %d / %d", index, columnsTotal)));
                }

                if (index >= columnsList.size()) {
                    informer.accept(Component.translatable("imm_ptl.finished_clearing_border"));
                    return true;
                }

                return false;
            }

            @Override
            public void onCancelled() {
                // nothing
            }
        };

        qouteall.imm_ptl.core.IPGlobal.serverTaskList.addTask(task);
    }
}