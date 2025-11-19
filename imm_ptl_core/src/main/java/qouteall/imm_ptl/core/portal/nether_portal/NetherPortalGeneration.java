package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;
import qouteall.imm_ptl.core.chunk_loading.NewChunkTrackingGraph;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class NetherPortalGeneration {
    // configurable timeouts and limits
    private static final int PORTAL_CHUNK_WAIT_TIMEOUT_TICKS = 400; // ~20 seconds
    private static final int PORTAL_CHUNK_REMOVAL_DELAY_TICKS = 12000; // ~10 minutes
    private static final int PORTAL_CHUNK_FINALIZER_SCHEDULE_TICKS = 40;

    @Nullable
    public static IntBox findAirCubePlacement(
        ServerLevel toWorld,
        BlockPos mappedPosInOtherDimension,
        Direction.Axis axis,
        BlockPos neededAreaSize,
        boolean allowForcePlacement
    ) {
        BlockPos randomShift = new BlockPos(
            toWorld.getRandom().nextBoolean() ? 1 : -1,
            0,
            toWorld.getRandom().nextBoolean() ? 1 : -1
        );

        IntBox foundAirCube =
            axis == Direction.Axis.Y ?
                NetherPortalMatcher.findHorizontalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension.offset(randomShift)
                ) :
                NetherPortalMatcher.findVerticalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension.offset(randomShift)
                );

        if (foundAirCube == null) {
            Helper.log("Cannot find normal portal placement");
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                neededAreaSize, toWorld, mappedPosInOtherDimension, 32
            );

            if (foundAirCube != null) {
                if (isFloating(toWorld, foundAirCube)) {
                    foundAirCube = NetherPortalMatcher.levitateBox(toWorld, foundAirCube, 50);
                }
            }
        }

        if (foundAirCube == null) {
            if (allowForcePlacement) {
                Helper.err("Cannot find air cube within 32 blocks? " +
                    "Force placed portal. It will occupy normal blocks.");
    
                return IntBox.fromBasePointAndSize(mappedPosInOtherDimension, neededAreaSize);
            }
            else {
                return null;
            }
        }
        return foundAirCube;
    }
    
    private static boolean isFloating(ServerLevel toWorld, IntBox foundAirCube) {
        return foundAirCube.getSurfaceLayer(Direction.DOWN).stream().noneMatch(
            blockPos -> toWorld.getBlockState(blockPos.below()).isSolid()
        );
    }

    public static void setPortalContentBlock(
        ServerLevel world,
        BlockPos pos,
        Direction.Axis normalAxis
    ) {
        world.setBlockAndUpdate(
            pos,
            PortalPlaceholderBlock.instance.defaultBlockState().setValue(
                PortalPlaceholderBlock.AXIS, normalAxis
            )
        );
    }

    public static void startGeneratingPortal(
        ServerLevel fromWorld, ServerLevel toWorld,
        BlockPortalShape fromShape,
        BlockPos toPos,
        int existingFrameSearchingRadius,
        Predicate<BlockState> otherSideFramePredicate,
        Consumer<BlockPortalShape> newFrameGenerateFunc,
        Consumer<PortalGenInfo> portalEntityGeneratingFunc,
        //return null for not generate new frame
        Supplier<PortalGenInfo> newFramePlacer,
        BooleanSupplier portalIntegrityChecker,

        //currying
        Function<WorldGenRegion, Function<BlockPos.MutableBlockPos, PortalGenInfo>> matchShapeByFramePos
    ) {
        ResourceKey<Level> fromDimension = fromWorld.dimension();
        ResourceKey<Level> toDimension = toWorld.dimension();

        Vec3 indicatorPos = fromShape.innerAreaBox.getCenterVec();

        LoadingIndicatorEntity indicatorEntity =
            LoadingIndicatorEntity.entityType.create(fromWorld);
        indicatorEntity.isValid = true;
        indicatorEntity.portalShape = fromShape;
        indicatorEntity.setPos(
            indicatorPos.x, indicatorPos.y, indicatorPos.z
        );
        fromWorld.addFreshEntity(indicatorEntity);

        Runnable onGenerateNewFrame = () -> {
            indicatorEntity.inform(Component.translatable(
                "imm_ptl.generating_new_frame"
            ));

            PortalGenInfo info = newFramePlacer.get();

            if (info != null) {
                newFrameGenerateFunc.accept(info.toShape);

                portalEntityGeneratingFunc.accept(info);

                O_O.postPortalSpawnEventForge(info);
            }
        };

        boolean otherSideChunkAlreadyGenerated = McHelper.getDoesRegionFileExist(toDimension, toPos);

        int frameSearchingRadius = Math.floorDiv(existingFrameSearchingRadius, 16) + 1;

        /**
         * if the other side chunk is already generated, generate 128 range for searching the frame
         * if the other side chunk is not yet generated, generate 1 or 2 chunk range for searching the frame placing position
         * when generating chunks by getBlockState, subsequent setBlockState may leave lighting issues
         * {@link net.minecraft.server.world.ServerLightingProvider#light(Chunk, boolean)}
         *  may get invoked twice for a chunk.
         * Maybe related to https://bugs.mojang.com/browse/MC-170010
         * Rough experiments shows that the lighting issue won't possibly manifest when manipulating blocks
         *  after the chunk has been fully generated.
         */
        int loaderRadius = otherSideChunkAlreadyGenerated ?
             frameSearchingRadius :
             (fromShape.getShapeInnerLength() < 16 ? 1 : 2);
         ChunkLoader chunkLoader = new ChunkLoader(
             new DimensionalChunkPos(toDimension, new ChunkPos(toPos)), loaderRadius
         );

        final boolean loaderAdded = !otherSideChunkAlreadyGenerated;
        if (loaderAdded) {
             NewChunkTrackingGraph.addGlobalAdditionalChunkLoader(chunkLoader);
            // ensure pending marks are applied immediately so other mods (distant-horizon terrain gen) see the tickets
            NewChunkTrackingGraph.applyAllPendingTicketMarksNow();
            Helper.log(String.format("NetherPortalGeneration: forced apply pending marks after adding chunkLoader center=%s radius=%d",
                chunkLoader.center, chunkLoader.radius));
             Helper.log(String.format("NetherPortalGeneration: added chunkLoader center=%s radius=%d otherSideChunkAlreadyGenerated=%s",
                 chunkLoader.center, chunkLoader.radius, otherSideChunkAlreadyGenerated));
         }

         Runnable finalizer = () -> {
            Helper.log(String.format("NetherPortalGeneration: finalizer running, scheduling removal of chunkLoader center=%s radius=%d in 40 ticks",
                chunkLoader.center, chunkLoader.radius));
            indicatorEntity.remove(Entity.RemovalReason.KILLED);

            // Schedule removal after a longer delay (debounce) to avoid world-gen threadpool thrashing
            if (loaderAdded) {
                MyTaskList.MyTask removalTask = MyTaskList.withDelay(PORTAL_CHUNK_REMOVAL_DELAY_TICKS, MyTaskList.oneShotTask(() -> {
                     Helper.log(String.format("NetherPortalGeneration: performing delayed removal of chunkLoader center=%s radius=%d",
                         chunkLoader.center, chunkLoader.radius));
                     NewChunkTrackingGraph.removeGlobalAdditionalChunkLoader(chunkLoader);
                 }));

                 IPGlobal.serverTaskList.addTask(removalTask);
            }
          };

        // Use an explicit MyTask so we can implement a timeout fallback to avoid permanent hangs
        MyTaskList.MyTask portalTask = new MyTaskList.MyTask() {
            int waitingTicks = 0;
            final int timeoutTicks = PORTAL_CHUNK_WAIT_TIMEOUT_TICKS; // ~20 seconds

            @Override
            public boolean runAndGetIsFinished() {
                boolean isPortalIntact = portalIntegrityChecker.getAsBoolean();

                if (!isPortalIntact) {
                    finalizer.run();
                    return true;
                }

                int loadedChunks = chunkLoader.getLoadedChunkNum();
                int allChunksNeedsLoading = chunkLoader.getChunkNum();

                if (loadedChunks < allChunksNeedsLoading) {
                    waitingTicks++;
                    limitedLogger.log(String.format("NetherPortalGeneration: waiting for chunks %d/%d for portal at %s (tick %d/%d)",
                        loadedChunks, allChunksNeedsLoading, toPos, waitingTicks, timeoutTicks));
                    indicatorEntity.inform(Component.translatable(
                        "imm_ptl.loading_chunks", loadedChunks, allChunksNeedsLoading
                    ));

                    if (waitingTicks > timeoutTicks) {
                        Helper.log(String.format("NetherPortalGeneration: timeout waiting for chunks for portal at %s, falling back to generating new frame", toPos));
                        onGenerateNewFrame.run();
                        finalizer.run();
                        return true;
                    }

                    return false;
                }

                // reset ticks if loading progressed to finished
                waitingTicks = 0;

                if (!otherSideChunkAlreadyGenerated) {
                    onGenerateNewFrame.run();
                    finalizer.run();
                    return true;
                }

                try {
                    indicatorEntity.inform(Component.translatable("imm_ptl.searching_for_frame"));

                    if (otherSideChunkAlreadyGenerated) {
                        // Build chunk list directly from the world and search without creating a WorldGenRegion
                        ServerLevel targetWorld = toWorld;
                        ArrayList<net.minecraft.world.level.chunk.ChunkAccess> chunks = new ArrayList<>();
                        for (int dz = -frameSearchingRadius; dz <= frameSearchingRadius; dz++) {
                            for (int dx = -frameSearchingRadius; dx <= frameSearchingRadius; dx++) {
                                int cx = chunkLoader.center.x + dx;
                                int cz = chunkLoader.center.z + dz;
                                // use non-creating accessor to avoid triggering chunk generation
                                net.minecraft.world.level.chunk.ChunkAccess c = McHelper.getServerChunkIfPresent(targetWorld, cx, cz);
                                if (c != null) {
                                    chunks.add(c);
                                }
                            }
                        }

                        // Create a LenientChunkRegion so the matchShape function has correct context
                        // create a lenient region; it will not load missing chunks
                        qouteall.imm_ptl.core.chunk_loading.LenientChunkRegion region =
                            qouteall.imm_ptl.core.chunk_loading.LenientChunkRegion.createLenientChunkRegion(
                                chunkLoader.center, frameSearchingRadius, toWorld
                            );

                        FrameSearching.startSearchingPortalFrameAsyncFromChunks(
                            toWorld, chunks, toPos, frameSearchingRadius,
                            otherSideFramePredicate,
                            matchShapeByFramePos.apply(region),
                            (info) -> {
                                portalEntityGeneratingFunc.accept(info);
                                finalizer.run();
                                O_O.postPortalSpawnEventForge(info);
                            },
                            () -> {
                                onGenerateNewFrame.run();
                                finalizer.run();
                            }
                        );

                        return true;
                    }

                    WorldGenRegion chunkRegion = new ChunkLoader(
                        chunkLoader.center, frameSearchingRadius
                    ).createChunkRegion();

                    FrameSearching.startSearchingPortalFrameAsync(
                        chunkRegion, frameSearchingRadius,
                        toPos, otherSideFramePredicate,
                        matchShapeByFramePos.apply(chunkRegion),
                        (info) -> {
                            portalEntityGeneratingFunc.accept(info);
                            finalizer.run();

                            O_O.postPortalSpawnEventForge(info);
                        },
                        () -> {
                            onGenerateNewFrame.run();
                            finalizer.run();
                        });

                    return true;
                }
                catch (Throwable t) {
                    Helper.logger.error("NetherPortalGeneration: exception while creating chunkRegion or searching for frame, falling back to generate new frame", t);
                    // fallback to generating a new frame to avoid permanent hang
                    onGenerateNewFrame.run();
                    finalizer.run();
                    return true;
                }
            }

            @Override
            public void onCancelled() {
                // nothing
            }
        };

        IPGlobal.serverTaskList.addTask(portalTask);
    }

    public static boolean isOtherGenerationRunning(ServerLevel fromWorld, Vec3 indicatorPos) {

        boolean isOtherGenerationRunning = McHelper.getEntitiesNearby(
            fromWorld, indicatorPos, LoadingIndicatorEntity.class, 1
        ).stream().findAny().isPresent();
        if (isOtherGenerationRunning) {
            Helper.log(
                "Aborted Portal Generation Because Another Generation is Running Nearby"
            );
            return true;
        }
        return false;
    }

    private static final LimitedLogger limitedLogger = new LimitedLogger(50);

    public static boolean checkPortalGeneration(ServerLevel fromWorld, BlockPos startingPos) {
        if (!fromWorld.hasChunkAt(startingPos)) {
            Helper.log("Cancel Portal Generation Because Chunk Not Loaded");
            return false;
        }

        limitedLogger.log(String.format("Portal Generation Attempted %s %s %s %s",
            fromWorld.dimension().location(), startingPos.getX(), startingPos.getY(), startingPos.getZ()
        ));
        return true;
    }

    public static BlockPortalShape findFrameShape(
        ServerLevel fromWorld, BlockPos startingPos,
        Predicate<BlockState> thisSideAreaPredicate,
        Predicate<BlockState> thisSideFramePredicate
    ) {
        return Arrays.stream(Direction.Axis.values())
            .map(
                axis -> {
                    return BlockPortalShape.findShapeWithoutRegardingStartingPos(
                        startingPos,
                        axis,
                        (pos) -> thisSideAreaPredicate.test(fromWorld.getBlockState(pos)),
                        (pos) -> thisSideFramePredicate.test(fromWorld.getBlockState(pos))
                    );
                }
            ).filter(
                Objects::nonNull
            ).findFirst().orElse(null);
    }

    public static void embodyNewFrame(
        ServerLevel toWorld,
        BlockPortalShape toShape,
        BlockState frameBlockState
    ) {
        toShape.frameAreaWithCorner.forEach(blockPos ->
            toWorld.setBlockAndUpdate(blockPos, frameBlockState)
        );
    }

    public static void fillInPlaceHolderBlocks(
        ServerLevel world,
        BlockPortalShape blockPortalShape
    ) {
        blockPortalShape.area.forEach(
            blockPos -> setPortalContentBlock(
                world, blockPos, blockPortalShape.axis
            )
        );
    }


}
