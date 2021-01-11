package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibilityManager;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class NetherPortalGeneration {
    
    public static IntBox findAirCubePlacement(
        ServerWorld toWorld,
        BlockPos mappedPosInOtherDimension,
        Direction.Axis axis,
        BlockPos neededAreaSize
    ) {
        BlockPos randomShift = new BlockPos(
            toWorld.getRandom().nextBoolean() ? 1 : -1,
            0,
            toWorld.getRandom().nextBoolean() ? 1 : -1
        );
        
        IntBox foundAirCube =
            axis == Direction.Axis.Y ?
                NetherPortalMatcher.findHorizontalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension.add(randomShift)
                ) :
                NetherPortalMatcher.findVerticalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension.add(randomShift)
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
            Helper.err("Cannot find air cube within 32 blocks? " +
                "Force placed portal. It will occupy normal blocks.");
            
            foundAirCube = IntBox.getBoxByBasePointAndSize(
                neededAreaSize,
                mappedPosInOtherDimension
            );
        }
        return foundAirCube;
    }
    
    private static boolean isFloating(ServerWorld toWorld, IntBox foundAirCube) {
        return foundAirCube.getSurfaceLayer(Direction.DOWN).stream().noneMatch(
            blockPos -> toWorld.getBlockState(blockPos.down()).getMaterial().isSolid()
        );
    }
    
    public static void setPortalContentBlock(
        ServerWorld world,
        BlockPos pos,
        Direction.Axis normalAxis
    ) {
        world.setBlockState(
            pos,
            PortalPlaceholderBlock.instance.getDefaultState().with(
                PortalPlaceholderBlock.AXIS, normalAxis
            )
        );
    }
    
    public static void startGeneratingPortal(
        ServerWorld fromWorld, ServerWorld toWorld,
        BlockPortalShape fromShape,
        BlockPos toPos,
        int existingFrameSearchingRadius,
        Predicate<BlockState> otherSideFramePredicate,
        Consumer<BlockPortalShape> newFrameGenerateFunc, Consumer<PortalGenInfo> portalEntityGeneratingFunc,
        //return null for not generate new frame
        Supplier<PortalGenInfo> newFramePlacer,
        BooleanSupplier portalIntegrityChecker,
        
        //currying
        Function<ChunkRegion, Function<BlockPos.Mutable, PortalGenInfo>> matchShapeByFramePos
    ) {
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        RegistryKey<World> toDimension = toWorld.getRegistryKey();
        
        Vec3d indicatorPos = fromShape.innerAreaBox.getCenterVec();
        
        LoadingIndicatorEntity indicatorEntity =
            LoadingIndicatorEntity.entityType.create(fromWorld);
        indicatorEntity.isValid = true;
        indicatorEntity.portalShape = fromShape;
        indicatorEntity.updatePosition(
            indicatorPos.x, indicatorPos.y, indicatorPos.z
        );
        fromWorld.spawnEntity(indicatorEntity);
        
        Runnable onGenerateNewFrame = () -> {
            indicatorEntity.inform(new TranslatableText(
                "imm_ptl.generating_new_frame"
            ));
            
            PortalGenInfo info = newFramePlacer.get();
            
            if (info != null) {
                newFrameGenerateFunc.accept(info.toShape);
                
                portalEntityGeneratingFunc.accept(info);
                
                O_O.postPortalSpawnEventForge(info);
            }
        };
        
        boolean shouldSkip = !McHelper.getIEStorage(toDimension)
            .portal_isChunkGenerated(new ChunkPos(toPos));
        if (shouldSkip) {
            Helper.log("Skip Portal Frame Searching Because The Region is not Generated");
            onGenerateNewFrame.run();
            indicatorEntity.remove();
            return;
        }
        
        int loaderRadius = Math.floorDiv(existingFrameSearchingRadius, 16) + 1;
        ChunkVisibilityManager.ChunkLoader chunkLoader = new ChunkVisibilityManager.ChunkLoader(
            new DimensionalChunkPos(toDimension, new ChunkPos(toPos)), loaderRadius
        );
        
        NewChunkTrackingGraph.addAdditionalChunkLoader(chunkLoader);
        
        Runnable finalizer = () -> {
            indicatorEntity.remove();
            NewChunkTrackingGraph.removeAdditionalChunkLoader(chunkLoader);
        };
        
        ModMain.serverTaskList.addTask(() -> {
            
            boolean isPortalIntact = portalIntegrityChecker.getAsBoolean();
            
            if (!isPortalIntact) {
                finalizer.run();
                return true;
            }
            
            int[] loadedChunkNum = {0};
            chunkLoader.foreachChunkPos((dim, x, z, dist) -> {
                WorldChunk chunk = McHelper.getServerChunkIfPresent(dim, x, z);
                if (chunk != null) {
                    loadedChunkNum[0] += 1;
                }
            });
            
            int allChunksNeedsLoading = (loaderRadius * 2 + 1) * (loaderRadius * 2 + 1);
            
            if (allChunksNeedsLoading > loadedChunkNum[0]) {
                indicatorEntity.inform(new TranslatableText(
                    "imm_ptl.loading_chunks", loadedChunkNum[0], allChunksNeedsLoading
                ));
                return false;
            }
            else {
                ChunkRegion chunkRegion = chunkLoader.createChunkRegion();
                
                indicatorEntity.inform(new TranslatableText("imm_ptl.searching_for_frame"));
                
                BlockPos.Mutable temp1 = new BlockPos.Mutable();
                
                FrameSearching.startSearchingPortalFrameAsync(
                    chunkRegion, loaderRadius,
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
        });
    }
    
    public static boolean isOtherGenerationRunning(ServerWorld fromWorld, Vec3d indicatorPos) {
        
        boolean isOtherGenerationRunning = McHelper.getEntitiesNearby(
            fromWorld, indicatorPos, LoadingIndicatorEntity.class, 1
        ).findAny().isPresent();
        if (isOtherGenerationRunning) {
            Helper.log(
                "Aborted Portal Generation Because Another Generation is Running Nearby"
            );
            return true;
        }
        return false;
    }
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(300);
    
    public static boolean checkPortalGeneration(ServerWorld fromWorld, BlockPos startingPos) {
        if (!fromWorld.isChunkLoaded(startingPos)) {
            Helper.log("Cancel Portal Generation Because Chunk Not Loaded");
            return false;
        }
        
        limitedLogger.log(String.format("Portal Generation Attempted %s %s %s %s",
            fromWorld.getRegistryKey().getValue(), startingPos.getX(), startingPos.getY(), startingPos.getZ()
        ));
        return true;
    }
    
    public static BlockPortalShape findFrameShape(
        ServerWorld fromWorld, BlockPos startingPos,
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
        ServerWorld toWorld,
        BlockPortalShape toShape,
        BlockState frameBlockState
    ) {
        toShape.frameAreaWithCorner.forEach(blockPos ->
            toWorld.setBlockState(blockPos, frameBlockState)
        );
    }
    
    public static void fillInPlaceHolderBlocks(
        ServerWorld world,
        BlockPortalShape blockPortalShape
    ) {
        blockPortalShape.area.forEach(
            blockPos -> setPortalContentBlock(
                world, blockPos, blockPortalShape.axis
            )
        );
    }
    
    
}
