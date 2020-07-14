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
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetherPortalGeneration {
    public final static int randomShiftFactor = 20;
    
    public static BlockPos getRandomShift() {
        Random rand = new Random();
        return new BlockPos(
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor
        );
    }
    
    public static IntBox findAirCubePlacement(
        ServerWorld toWorld,
        BlockPos mappedPosInOtherDimension,
        Direction.Axis axis,
        BlockPos neededAreaSize,
        int findingRadius
    ) {
        IntBox foundAirCube =
            axis == Direction.Axis.Y ?
                NetherPortalMatcher.findHorizontalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension,
                    findingRadius
                ) :
                NetherPortalMatcher.findVerticalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension,
                    findingRadius
                );
        
        if (foundAirCube == null) {
            Helper.log("Cannot find normal portal placement");
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                neededAreaSize, toWorld, mappedPosInOtherDimension, 10
            );
        }
        
        if (foundAirCube == null) {
            Helper.log("Cannot find air cube within 10 blocks");
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                neededAreaSize, toWorld, mappedPosInOtherDimension, 16
            );
        }
        
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
    public static void generateBreakablePortalEntities(
        Info info,
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
    }
    
    public static class Info {
        public RegistryKey<World> from;
        public RegistryKey<World> to;
        public BlockPortalShape fromShape;
        public BlockPortalShape toShape;
        
        public Info(
            RegistryKey<World> from,
            RegistryKey<World> to,
            BlockPortalShape fromShape,
            BlockPortalShape toShape
        ) {
            this.from = from;
            this.to = to;
            this.fromShape = fromShape;
            this.toShape = toShape;
        }
    }
    
    //return null for not found
    //executed on main server thread
    public static boolean onFireLitOnObsidian(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        
        RegistryKey<World> toDimension = getDestinationDimension(fromDimension);
        
        if (toDimension == null) return false;
        
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        
        int searchingRadius = Global.netherPortalFindingRadius;
        
        if (Global.reversibleNetherPortalLinking) {
            if (fromDimension == World.OVERWORLD) {
                searchingRadius /= 8;
            }
        }
        
        BlockPortalShape thisSideShape = triggerGeneratingPortal(
            fromWorld,
            firePos,
            toWorld,
            searchingRadius,
            searchingRadius - 10,
            (fromPos1) -> mapPosition(
                fromPos1,
                fromWorld.getRegistryKey(),
                toWorld.getRegistryKey()
            ),
            //this side area
            NetherPortalMatcher::isAirOrFire,
            //this side frame
            O_O::isObsidian,
            //other side area
            BlockState::isAir,
            //other side frame
            O_O::isObsidian,
            (shape) -> embodyNewFrame(toWorld, shape, Blocks.OBSIDIAN.getDefaultState()),
            info -> generateBreakablePortalEntities(info, NetherPortalEntity.entityType)
        );
        return thisSideShape != null;
    }
    
    public static boolean activatePortalHelper(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        Helper.SimpleBox<BlockPortalShape> thisSideShape = new Helper.SimpleBox<>(null);
        thisSideShape.obj = triggerGeneratingPortal(
            fromWorld,
            firePos,
            fromWorld,
            Global.netherPortalFindingRadius,
            Global.netherPortalFindingRadius,
            (fromPos1) -> getRandomShift().add(fromPos1),
            NetherPortalMatcher::isAirOrFire,
            blockState -> blockState.getBlock() == ModMain.portalHelperBlock,
            BlockState::isAir,
            (blockState) -> blockState.getBlock() == ModMain.portalHelperBlock,
            (toShape) -> {
                embodyNewFrame(fromWorld, toShape, ModMain.portalHelperBlock.getDefaultState());
            },
            info -> {
                generateHelperPortalEntities(info);
                info.fromShape.frameAreaWithCorner.forEach(blockPos -> {
                    if (fromWorld.getBlockState(blockPos).getBlock() == ModMain.portalHelperBlock) {
                        fromWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                    }
                });
            }
        );
        return thisSideShape.obj != null;
    }
    
    public static BlockPortalShape triggerGeneratingPortal(
        ServerWorld fromWorld,
        BlockPos startingPos,
        ServerWorld toWorld,
        int existingFrameSearchingRadius,
        int airCubeSearchingRadius,
        Function<BlockPos, BlockPos> positionMapping,
        Predicate<BlockState> thisSideAreaPredicate,
        Predicate<BlockState> thisSideFramePredicate,
        Predicate<BlockState> otherSideAreaPredicate,
        Predicate<BlockState> otherSideFramePredicate,
        Consumer<BlockPortalShape> newFrameGenerateFunc,
        Consumer<Info> portalEntityGeneratingFunc
    ) {
        if (!checkPortalGeneration(fromWorld, startingPos)) {
            return null;
        }
        
        if (!thisSideAreaPredicate.test(fromWorld.getBlockState(startingPos))) {
            return null;
        }
        
        BlockPortalShape fromShape =
            findFrameShape(fromWorld, startingPos, thisSideAreaPredicate, thisSideFramePredicate);
        
        if (fromShape == null) {
            return null;
        }
        
        BlockPos toPos = positionMapping.apply(fromShape.innerAreaBox.getCenter());
        
        if (isOtherGenerationRunning(fromWorld, fromShape)) return null;
        
        startGeneratingPortal(
            fromWorld, toWorld, fromShape, toPos,
            existingFrameSearchingRadius,
            otherSideAreaPredicate,
            otherSideFramePredicate, newFrameGenerateFunc, portalEntityGeneratingFunc,
            () -> {
                IntBox airCubePlacement =
                    findAirCubePlacement(
                        toWorld, toPos,
                        fromShape.axis, fromShape.totalAreaBox.getSize(),
                        airCubeSearchingRadius
                    );
                
                BlockPortalShape toShape = fromShape.getShapeWithMovedAnchor(
                    airCubePlacement.l.subtract(
                        fromShape.totalAreaBox.l
                    ).add(fromShape.anchor)
                );
                
                return toShape;
            },
            () -> {
                return fromShape.isPortalIntact(
                    blockPos -> thisSideAreaPredicate.test(fromWorld.getBlockState(blockPos)),
                    blockPos -> thisSideFramePredicate.test(fromWorld.getBlockState(blockPos))
                );
            }
        );
        
        return fromShape;
    }
    
    public static void startGeneratingPortal(
        ServerWorld fromWorld, ServerWorld toWorld,
        BlockPortalShape fromShape, BlockPos toPos,
        int existingFrameSearchingRadius,
        Predicate<BlockState> otherSideAreaPredicate, Predicate<BlockState> otherSideFramePredicate,
        Consumer<BlockPortalShape> newFrameGenerateFunc, Consumer<Info> portalEntityGeneratingFunc,
        //return null for not generate new frame
        Supplier<BlockPortalShape> newFramePlacer,
        BooleanSupplier portalIntegrityChecker
    ) {
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        RegistryKey<World> toDimension = toWorld.getRegistryKey();
        
        Vec3d indicatorPos = fromShape.innerAreaBox.getCenterVec();
        
        LoadingIndicatorEntity indicatorEntity =
            LoadingIndicatorEntity.entityType.create(fromWorld);
        indicatorEntity.isAlive = true;
        indicatorEntity.updatePosition(
            indicatorPos.x, indicatorPos.y, indicatorPos.z
        );
        fromWorld.spawnEntity(indicatorEntity);
        
        Runnable onGenerateNewFrame = () -> {
            indicatorEntity.setText(new TranslatableText(
                "imm_ptl.generating_new_frame"
            ));
            
            BlockPortalShape toShape = newFramePlacer.get();
            
            if (toShape != null) {
                newFrameGenerateFunc.accept(toShape);
                
                Info info = new Info(
                    fromDimension, toWorld.getRegistryKey(), fromShape, toShape
                );
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
            new DimensionalChunkPos(
                toDimension, new ChunkPos(toPos)
            ),
            loaderRadius
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
                indicatorEntity.setText(new TranslatableText(
                    "imm_ptl.loading_chunks", loadedChunkNum[0], allChunksNeedsLoading
                ));
                return false;
            }
            else {
                ChunkRegion chunkRegion = chunkLoader.createChunkRegion();
                
                indicatorEntity.setText(new TranslatableText("imm_ptl.searching_for_frame"));
                
                FrameSearching.startSearchingPortalFrameAsync(
                    chunkRegion, loaderRadius,
                    toPos, fromShape,
                    otherSideAreaPredicate, otherSideFramePredicate,
                    (shape) -> {
                        Info info = new Info(
                            fromDimension, toDimension, fromShape, shape
                        );
                        
                        portalEntityGeneratingFunc.accept(info);
                        finalizer.run();
                    },
                    () -> {
                        onGenerateNewFrame.run();
                        finalizer.run();
                    }
                );
                
                return true;
            }
        });
    }
    
    public static boolean isOtherGenerationRunning(ServerWorld fromWorld, BlockPortalShape foundShape) {
        Vec3d indicatorPos = foundShape.innerAreaBox.getCenterVec();
        
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
    
    public static boolean checkPortalGeneration(ServerWorld fromWorld, BlockPos startingPos) {
        if (!fromWorld.isChunkLoaded(startingPos)) {
            Helper.log("Cancel Nether Portal Generation Because Chunk Not Loaded");
            return false;
        }
        
        Helper.log(String.format("Portal Generation Attempted %s %s %s %s",
            fromWorld.getRegistryKey(), startingPos.getX(), startingPos.getY(), startingPos.getZ()
        ));
        return true;
    }
    
    public static BlockPortalShape findFrameShape(ServerWorld fromWorld, BlockPos startingPos, Predicate<BlockState> thisSideAreaPredicate, Predicate<BlockState> thisSideFramePredicate) {
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
    
    private static void generateHelperPortalEntities(Info info) {
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
        
        if (Global.blameOpenJdk) {
            return blockPosStreamNaive(
                world, x, z, range
            );
        }
        
        int height = world.getDimensionHeight();
        
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
