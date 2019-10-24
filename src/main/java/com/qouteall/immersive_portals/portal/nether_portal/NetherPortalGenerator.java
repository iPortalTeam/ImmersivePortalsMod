package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

//import com.sun.istack.internal.NotNull;
//import com.sun.istack.internal.Nullable;

public class NetherPortalGenerator {
    public final static int randomShiftFactor = 20;
    
    public static void spawnLoadingIndicator(
        ServerWorld world,
        ObsidianFrame obsidianFrame
    ) {
        IntegerAABBInclusive box = obsidianFrame.boxWithoutObsidian;
        Vec3d center = new Vec3d(
            (double) (box.h.getX() + box.l.getX() + 1) / 2,
            (double) (box.h.getY() + box.l.getY() + 1) / 2 - 1,
            (double) (box.h.getZ() + box.l.getZ() + 1) / 2
        );
        CustomPayloadS2CPacket packet =
            MyNetwork.createSpawnLoadingIndicator(world.dimension.getType(), center);
        Helper.getEntitiesNearby(
            world, center, ServerPlayerEntity.class, 64
        ).forEach(
            player -> player.networkHandler.sendPacket(packet)
        );
    }
    
    public static class NetherPortalGeneratedInformation {
        public DimensionType fromDimension;
        public DimensionType toDimension;
        public ObsidianFrame fromObsidianFrame;
        public ObsidianFrame toObsidianFrame;
        
        public NetherPortalGeneratedInformation(
            DimensionType fromDimension,
            DimensionType toDimension,
            ObsidianFrame fromObsidianFrame,
            ObsidianFrame toObsidianFrame
        ) {
            this.fromDimension = fromDimension;
            this.toDimension = toDimension;
            this.fromObsidianFrame = fromObsidianFrame;
            this.toObsidianFrame = toObsidianFrame;
        }
    }
    
    public static final SignalArged<NetherPortalGeneratedInformation> signalNetherPortalLit =
        new SignalArged<>();
    
    public static NetherPortalGeneratedInformation onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        DimensionType toDimension = getDestinationDimension(fromDimension);
        
        if (toDimension == null) return null;
        
        ObsidianFrame fromObsidianFrame = findTheObsidianFrameThatIsLighted(fromWorld, firePos);
        
        if (fromObsidianFrame == null) return null;
    
        ServerWorld toWorld = Helper.getServer().getWorld(toDimension);
    
        assert toWorld != null;
    
        spawnLoadingIndicator(fromWorld, fromObsidianFrame);
        
        BlockPos posInOtherDimension = getPosInOtherDimension(
            firePos, fromDimension, toDimension
        );
    
        ObsidianFrame toObsidianFrame = findExistingEmptyObsidianFrameWithSameSizeInDestDimension(
            fromObsidianFrame, toWorld, posInOtherDimension,
            NetherPortalMatcher.findingRadius
        );
        
        IntegerAABBInclusive heightLimitInOtherDimension =
            NetherPortalMatcher.getHeightLimit(toDimension);
        
        if (toObsidianFrame == null) {
            BlockPos randomShift = getRandomShift(fromDimension);
            toObsidianFrame = createObsidianFrameInOtherDimension(
                fromObsidianFrame, toWorld, posInOtherDimension.add(randomShift),
                heightLimitInOtherDimension
            );
        }
    
        registerPortalAndGenerateContentBlocks(
            fromWorld, fromObsidianFrame,
            toWorld, toObsidianFrame
        );
        
        NetherPortalGeneratedInformation information = new NetherPortalGeneratedInformation(
            fromDimension, toDimension,
            fromObsidianFrame, toObsidianFrame
        );
        signalNetherPortalLit.emit(information);
        
        return information;
    }
    
    private static BlockPos getRandomShift(DimensionType fromDimension) {
        Random rand = Helper.getServer().getWorld(fromDimension).random;
        return new BlockPos(
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor
        );
    }
    
    private static void registerPortalAndGenerateContentBlocks(
        ServerWorld fromWorld,
        ObsidianFrame fromObsidianFrame,
        ServerWorld toWorld,
        ObsidianFrame toObsidianFrame
    ) {
        registerPortalAndAllocateId(
            fromWorld, fromObsidianFrame,
            toWorld, toObsidianFrame
        );
        
        generatePortalContentBlocks(
            fromWorld, fromObsidianFrame
        );
        
        generatePortalContentBlocks(
            toWorld, toObsidianFrame
        );
    }
    
    //@NotNull
    private static ObsidianFrame createObsidianFrameInOtherDimension(
        ObsidianFrame fromObsidianFrame,
        ServerWorld toWorld,
        BlockPos mappedPosInOtherDimension,
        IntegerAABBInclusive heightLimit
    ) {
        BlockPos neededAreaSize = ObsidianFrame.expandToIncludeObsidianBlocks(
            fromObsidianFrame.normalAxis,
            fromObsidianFrame.boxWithoutObsidian
        ).getSize();
    
        IntegerAABBInclusive foundAirCube =
            fromObsidianFrame.normalAxis == Direction.Axis.Y ?
                NetherPortalMatcher.findHorizontalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension,
                    heightLimit, NetherPortalMatcher.findingRadius
                ) :
                NetherPortalMatcher.findVerticalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension,
                    heightLimit, NetherPortalMatcher.findingRadius
                );
        
        if (foundAirCube == null) {
            Helper.log("Cannot find normal portal placement");
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                neededAreaSize,
                toWorld,
                mappedPosInOtherDimension, heightLimit, NetherPortalMatcher.findingRadius
            );
        }
        
        if (foundAirCube == null) {
            Helper.err("No place to put portal? " +
                "Force placed portal. It will occupy normal blocks.");
            
            foundAirCube = IntegerAABBInclusive.getBoxByBasePointAndSize(
                neededAreaSize,
                mappedPosInOtherDimension
            );
        }
        
        ObsidianFrame toObsidianFrame = new ObsidianFrame(
            fromObsidianFrame.normalAxis,
            ObsidianFrame.shrinkToExcludeObsidianBlocks(
                fromObsidianFrame.normalAxis,
                foundAirCube
            )
        );
        
        generateObsidianFrame(
            toWorld,
            toObsidianFrame
        );
        
        return toObsidianFrame;
    }
    
    private static ObsidianFrame findExistingEmptyObsidianFrameWithSameSizeInDestDimension(
        ObsidianFrame fromObsidianFrame,
        ServerWorld toWorld,
        BlockPos posInOtherDimension,
        int findingRadius
    ) {
        return NetherPortalMatcher.findEmptyObsidianFrame(
            toWorld,
            posInOtherDimension,
            fromObsidianFrame.normalAxis,
            innerArea -> innerArea.getSize().equals(
                fromObsidianFrame.boxWithoutObsidian.getSize()
            ),
            findingRadius
        );
    }
    
    private static ObsidianFrame findTheObsidianFrameThatIsLighted(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        return Arrays.stream(
            Direction.Axis.values()
        ).map(
            axis -> NetherPortalMatcher.detectFrameFromInnerPos(
                fromWorld, firePos, axis, whatever -> true
            )
        ).filter(
            Objects::nonNull
        ).findFirst().orElse(null);
    }
    
    public static DimensionType getDestinationDimension(
        DimensionType fromDimension
    ) {
        if (fromDimension == DimensionType.OVERWORLD) {
            return DimensionType.THE_NETHER;
        }
        else if (fromDimension == DimensionType.THE_NETHER) {
            return DimensionType.OVERWORLD;
        }
        else {
            return null;
        }
    }
    
    private static BlockPos getPosInOtherDimension(
        BlockPos pos,
        DimensionType dimensionFrom,
        DimensionType dimensionTo
    ) {
        if (dimensionFrom == DimensionType.OVERWORLD && dimensionTo == DimensionType.THE_NETHER) {
            return new BlockPos(
                pos.getX() / 8,
                pos.getY(),
                pos.getZ() / 8
            );
        }
        else if (dimensionFrom == DimensionType.THE_NETHER && dimensionTo == DimensionType.OVERWORLD) {
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
    
    //it returns the primary id
    private static void registerPortalAndAllocateId(
        ServerWorld fromWorld,
        ObsidianFrame fromObsidianFrame,
        ServerWorld toWorld,
        ObsidianFrame toObsidianFrame
    ) {
        assert fromObsidianFrame.boxWithoutObsidian.getSize().equals(
            toObsidianFrame.boxWithoutObsidian.getSize()
        );
        
        assert fromObsidianFrame.normalAxis == toObsidianFrame.normalAxis;
        
        BlockPos innerAreaSize = fromObsidianFrame.boxWithoutObsidian.getSize();
        Direction.Axis normalAxis = fromObsidianFrame.normalAxis;
    
        Vec3d centerOffset = new Vec3d(innerAreaSize).multiply(0.5);
    
        NetherPortalEntity[] portalArray = new NetherPortalEntity[]{
            new NetherPortalEntity(fromWorld),
            new NetherPortalEntity(fromWorld),
            new NetherPortalEntity(toWorld),
            new NetherPortalEntity(toWorld)
        };
    
        Portal.initBiWayBiFacedPortal(
            portalArray,
            fromWorld.getDimension().getType(),
            new Vec3d(fromObsidianFrame.boxWithoutObsidian.l)
                .add(centerOffset),
            toWorld.getDimension().getType(),
            new Vec3d(toObsidianFrame.boxWithoutObsidian.l)
                .add(centerOffset),
            normalAxis,
            new Vec3d(innerAreaSize)
        );
    
        portalArray[0].obsidianFrame = fromObsidianFrame;
        portalArray[1].obsidianFrame = fromObsidianFrame;
    
        portalArray[2].obsidianFrame = toObsidianFrame;
        portalArray[3].obsidianFrame = toObsidianFrame;
    
        portalArray[0].reversePortalId = portalArray[3].getUuid();
        portalArray[3].reversePortalId = portalArray[0].getUuid();
    
        portalArray[1].reversePortalId = portalArray[2].getUuid();
        portalArray[2].reversePortalId = portalArray[1].getUuid();
        
        fromWorld.spawnEntity(portalArray[0]);
        fromWorld.spawnEntity(portalArray[1]);
        toWorld.spawnEntity(portalArray[2]);
        toWorld.spawnEntity(portalArray[3]);
        
    }
    
    private static void generateObsidianFrame(
        ServerWorld world,
        ObsidianFrame obsidianFrame
    ) {
        Direction.Axis axisOfNormal = obsidianFrame.normalAxis;
        IntegerAABBInclusive boxIncludingObsidianFrame =
            ObsidianFrame.expandToIncludeObsidianBlocks(
                axisOfNormal,
                obsidianFrame.boxWithoutObsidian
            );
        
        Arrays.stream(
            Helper.getAnotherFourDirections(axisOfNormal)
        ).forEach(
            facing -> boxIncludingObsidianFrame
                .getSurfaceLayer(facing)
                .stream()
                .forEach(
                    blockPos -> setObsidian(world, blockPos)
                )
        );
    }
    
    private static void generatePortalContentBlocks(
        ServerWorld world,
        ObsidianFrame obsidianFrame
    ) {
        IntegerAABBInclusive contentBlockArea =
            obsidianFrame.boxWithoutObsidian;
        
        contentBlockArea.stream().forEach(
            blockPos -> setPortalContentBlock(
                world,
                blockPos,
                obsidianFrame.normalAxis
            )
        );
    }
    
    private static void setObsidian(
        ServerWorld world,
        BlockPos pos
    ) {
        boolean result = world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState(), 1 | 2);
    }
    
    private static void setPortalContentBlock(
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
    
}
