package com.qouteall.immersive_portals.portal.nether_portal;

import com.google.common.collect.Streams;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibilityManager;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetherPortalGeneration {
    
    public static IntBox findAirCubePlacement(
        ServerWorld toWorld,
        BlockPos mappedPosInOtherDimension,
        Direction.Axis axis,
        BlockPos neededAreaSize
    ) {
        IntBox foundAirCube =
            axis == Direction.Axis.Y ?
                NetherPortalMatcher.findHorizontalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension
                ) :
                NetherPortalMatcher.findVerticalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension
                );
        
        if (foundAirCube == null) {
            Helper.log("Cannot find normal portal placement");
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                neededAreaSize, toWorld, mappedPosInOtherDimension, 16
            );
            
            if (foundAirCube != null) {
                if (isFloating(toWorld, foundAirCube)) {
                    foundAirCube = NetherPortalMatcher.levitateBox(toWorld, foundAirCube, 50);
                }
            }
        }

//        if (foundAirCube == null) {
//            Helper.log("Cannot find air cube within 12 blocks");
//            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
//                neededAreaSize, toWorld, mappedPosInOtherDimension, 16
//            );
//        }
//
        if (foundAirCube == null) {
            Helper.err("Cannot find air cube within 16 blocks? " +
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
    
    public static RegistryKey<World> getDestinationDimension(
        RegistryKey<World> fromDimension
    ) {
        if (fromDimension == World.NETHER) {
            return World.OVERWORLD;
        }
        else if (fromDimension == World.END) {
            return null;
        }
        else {
            //you can access nether in any other dimension
            //including alternate dimensions
            return World.NETHER;
        }
    }
    
    public static BlockPos mapPosition(
        BlockPos pos,
        RegistryKey<World> dimensionFrom,
        RegistryKey<World> dimensionTo
    ) {
        if (dimensionFrom == World.OVERWORLD && dimensionTo == World.NETHER) {
            return new BlockPos(
                Math.round(pos.getX() / 8.0),
                pos.getY(),
                Math.round(pos.getZ() / 8.0)
            );
        }
        else if (dimensionFrom == World.NETHER && dimensionTo == World.OVERWORLD) {
            return new BlockPos(
                pos.getX() * 8,
                pos.getY(),
                pos.getZ() * 8
            );
        }
        else {
            return pos;
        }
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
            ),
            3
        );
    }
    
    //create portal entity and generate placeholder blocks
    public static BreakablePortalEntity[] generateBreakablePortalEntitiesAndPlaceholder(
        PortalGenInfo info,
        EntityType<? extends BreakablePortalEntity> entityType
    ) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(info.from);
        ServerWorld toWorld = McHelper.getServer().getWorld(info.to);
        
        fillInPlaceHolderBlocks(fromWorld, info.fromShape);
        fillInPlaceHolderBlocks(toWorld, info.toShape);
        
        BreakablePortalEntity[] portalArray = new BreakablePortalEntity[]{
            entityType.create(fromWorld),
            entityType.create(fromWorld),
            entityType.create(toWorld),
            entityType.create(toWorld)
        };
        
        info.fromShape.initPortalPosAxisShape(
            portalArray[0], false
        );
        info.fromShape.initPortalPosAxisShape(
            portalArray[1], true
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[2], false
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[3], true
        );
        
        portalArray[0].dimensionTo = info.to;
        portalArray[1].dimensionTo = info.to;
        portalArray[2].dimensionTo = info.from;
        portalArray[3].dimensionTo = info.from;
        
        Vec3d offset = Vec3d.of(info.toShape.innerAreaBox.l.subtract(
            info.fromShape.innerAreaBox.l
        ));
        portalArray[0].destination = portalArray[0].getPos().add(offset);
        portalArray[1].destination = portalArray[1].getPos().add(offset);
        portalArray[2].destination = portalArray[2].getPos().subtract(offset);
        portalArray[3].destination = portalArray[3].getPos().subtract(offset);
        
        portalArray[0].blockPortalShape = info.fromShape;
        portalArray[1].blockPortalShape = info.fromShape;
        portalArray[2].blockPortalShape = info.toShape;
        portalArray[3].blockPortalShape = info.toShape;
        
        portalArray[0].reversePortalId = portalArray[2].getUuid();
        portalArray[1].reversePortalId = portalArray[3].getUuid();
        portalArray[2].reversePortalId = portalArray[0].getUuid();
        portalArray[3].reversePortalId = portalArray[1].getUuid();
        
        fromWorld.spawnEntity(portalArray[0]);
        fromWorld.spawnEntity(portalArray[1]);
        toWorld.spawnEntity(portalArray[2]);
        toWorld.spawnEntity(portalArray[3]);
        
        return portalArray;
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
            
            PortalGenInfo placedShape = newFramePlacer.get();
            
            if (placedShape != null) {
                newFrameGenerateFunc.accept(placedShape.toShape);
                
                portalEntityGeneratingFunc.accept(placedShape);
                
                O_O.postPortalSpawnEventForge(placedShape);
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
    
    public static Stream<BlockPos> blockPosStreamNaive(
        ServerWorld toWorld,
        int x, int z, int raidus
    ) {
        Stream<BlockPos> blockPosStream = BlockPos.stream(
            new BlockPos(
                x - raidus,
                3,
                z - raidus
            ),
            new BlockPos(
                x + raidus,
                toWorld.getDimensionHeight() - 3,
                z + raidus
            )
        );
        return blockPosStream;
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
    
    @Deprecated
    private static void generateHelperPortalEntities(PortalGenInfo info) {
        ServerWorld fromWorld1 = McHelper.getServer().getWorld(info.from);
        ServerWorld toWorld = McHelper.getServer().getWorld(info.to);
        
        Portal[] portalArray = new Portal[]{
            Portal.entityType.create(fromWorld1),
            Portal.entityType.create(fromWorld1),
            Portal.entityType.create(toWorld),
            Portal.entityType.create(toWorld)
        };
        
        info.fromShape.initPortalPosAxisShape(
            portalArray[0], false
        );
        info.fromShape.initPortalPosAxisShape(
            portalArray[1], true
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[2], false
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[3], true
        );
        
        portalArray[0].dimensionTo = info.to;
        portalArray[1].dimensionTo = info.to;
        portalArray[2].dimensionTo = info.from;
        portalArray[3].dimensionTo = info.from;
        
        Vec3d offset = Vec3d.of(info.toShape.innerAreaBox.l.subtract(
            info.fromShape.innerAreaBox.l
        ));
        portalArray[0].destination = portalArray[0].getPos().add(offset);
        portalArray[1].destination = portalArray[1].getPos().add(offset);
        portalArray[2].destination = portalArray[2].getPos().subtract(offset);
        portalArray[3].destination = portalArray[3].getPos().subtract(offset);
        
        fromWorld1.spawnEntity(portalArray[0]);
        fromWorld1.spawnEntity(portalArray[1]);
        toWorld.spawnEntity(portalArray[2]);
        toWorld.spawnEntity(portalArray[3]);
    }
    
    public static Stream<BlockPos> fromNearToFarColumned(
        ServerWorld world,
        int x,
        int z,
        int range
    ) {
        if (range < 0) {
            range = 5;
        }
        
        int height = world.getDimensionHeight();
        
        if (Global.blameOpenJdk) {
            return blockPosStreamNaive(
                world, x, z, range
            );
        }
        
        
        BlockPos.Mutable temp0 = new BlockPos.Mutable();
        BlockPos.Mutable temp2 = new BlockPos.Mutable();
        BlockPos.Mutable temp1 = new BlockPos.Mutable();
        
        return IntStream.range(0, range).boxed()
            .flatMap(layer ->
                Streams.concat(
                    IntStream.range(0, layer * 2 + 1)
                        .mapToObj(i -> new BlockPos(layer, 0, i - layer)),
                    IntStream.range(0, layer * 2 + 1)
                        .mapToObj(i -> new BlockPos(-layer, 0, i - layer)),
                    IntStream.range(0, layer * 2 - 1)
                        .mapToObj(i -> new BlockPos(i - layer + 1, 0, layer)),
                    IntStream.range(0, layer * 2 - 1)
                        .mapToObj(i -> new BlockPos(i - layer + 1, 0, -layer))
                ).map(
                    columnPos_ -> new BlockPos(columnPos_.getX() + x, 0, columnPos_.getZ() + z)
                ).flatMap(
                    columnPos_ ->
                        IntStream.range(3, height - 3)
                            .mapToObj(y -> new BlockPos(columnPos_.getX(), y, columnPos_.getZ()))
                )
            );
    }
    
}
