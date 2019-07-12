package com.qouteall.immersive_portals.nether_portal_managing;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.my_utils.Helper;
import com.qouteall.immersive_portals.my_utils.IntegerAABBInclusive;
import com.qouteall.immersive_portals.my_utils.SignalArged;
import com.qouteall.immersive_portals.portals.loading_indicator.LoadingIndicatorsManager;
import com.qouteall.immersive_portals.portals.portal_data.PortalDataManager;
import net.minecraft.block.Blocks;
import net.minecraft.init.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class NetherPortalGenerator {
    public final static int randomShiftFactor = 20;
    
    public static class NetherPortalGeneratedInformation {
        public DimensionType fromDimension;
        public DimensionType toDimension;
        public ObsidianFrame fromObsidianFrame;
        public ObsidianFrame toObsidianFrame;
        public int primaryPortalId;
        
        public NetherPortalGeneratedInformation(
            DimensionType fromDimension,
            DimensionType toDimension,
            ObsidianFrame fromObsidianFrame,
            ObsidianFrame toObsidianFrame,
            int primaryPortalId
        ) {
            this.fromDimension = fromDimension;
            this.toDimension = toDimension;
            this.fromObsidianFrame = fromObsidianFrame;
            this.toObsidianFrame = toObsidianFrame;
            this.primaryPortalId = primaryPortalId;
        }
    }
    
    public static final SignalArged<NetherPortalGeneratedInformation> signalNetherPortalLit =
        new SignalArged<>();
    
    @Nullable
    public static NetherPortalGeneratedInformation onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        //TODO optimize it. avoid test for 128 range
        
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        DimensionType toDimension = getDestinationDimension(fromDimension);
        
        if (toDimension == null) return null;
        
        ObsidianFrame fromObsidianFrame = findTheObsidianFrameThatIsLighted(fromWorld, firePos);
        
        if (fromObsidianFrame == null) return null;
    
        ServerWorld toWorld = Helper.getServer().getWorld(toDimension);
    
        assert toWorld != null;
        
        //TODO add loading indicator
//        LoadingIndicatorsManager.onNetherPortalAboutToGenerate(
//            fromObsidianFrame, fromDimension, toDimension
//        );
        
        BlockPos posInOtherDimension = getPosInOtherDimensionWithRandomShift(
            firePos, fromDimension, toDimension
        );
        
        ObsidianFrame toObsidianFrame = findExistingEmptyObsidianFrameWithSameSizeInDestDimension(
            fromObsidianFrame, toWorld, posInOtherDimension,
            NetherPortalMatcher.findingRadius + randomShiftFactor
        );
        
        IntegerAABBInclusive heightLimitInOtherDimension =
            NetherPortalMatcher.getHeightLimit(toDimension);
        
        if (toObsidianFrame == null) {
            toObsidianFrame = createObsidianFrameInOtherDimension(
                fromObsidianFrame, toWorld, posInOtherDimension,
                heightLimitInOtherDimension
            );
        }
        
        int primaryPortalId = registerPortalAndGenerateContentBlocks(
            fromWorld, fromObsidianFrame,
            toWorld, toObsidianFrame
        );
        
        NetherPortalGeneratedInformation information = new NetherPortalGeneratedInformation(
            fromDimension, toDimension,
            fromObsidianFrame, toObsidianFrame,
            primaryPortalId
        );
        signalNetherPortalLit.emit(
            information
        );
        
        return information;
    }
    
    private static int registerPortalAndGenerateContentBlocks(
        ServerWorld fromWorld,
        ObsidianFrame fromObsidianFrame,
        ServerWorld toWorld,
        ObsidianFrame toObsidianFrame
    ) {
        int primaryPortalId = registerPortalAndAllocateId(
            fromWorld, fromObsidianFrame,
            toWorld, toObsidianFrame
        );
        
        generatePortalContentBlocks(
            fromWorld, fromObsidianFrame,
            primaryPortalId
        );
        
        generatePortalContentBlocks(
            toWorld, toObsidianFrame,
            primaryPortalId
        );
        
        return primaryPortalId;
    }
    
    @Nonnull
    private static ObsidianFrame createObsidianFrameInOtherDimension(
        ObsidianFrame fromObsidianFrame,
        ServerWorld toWorld,
        BlockPos mappedPosInOtherDimension,
        IntegerAABBInclusive heightLimit
    ) {
        BlockPos needsAreaSize = ObsidianFrame.expandToIncludeObsidianBlocks(
            fromObsidianFrame.normalAxis,
            fromObsidianFrame.boxWithoutObsidian
        ).getSize();
        
        IntegerAABBInclusive foundAirCube = NetherPortalMatcher.findCubeAirAreaOnGround(
            needsAreaSize,
            toWorld,
            mappedPosInOtherDimension, heightLimit
        );
        
        if (foundAirCube == null) {
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                needsAreaSize,
                toWorld,
                mappedPosInOtherDimension, heightLimit
            );
        }
        
        if (foundAirCube == null) {
            Helper.err("No place to put portal in 128 range? " +
                "Force placed portal. It will occupy normal blocks.");
            
            foundAirCube = IntegerAABBInclusive.getBoxByBasePointAndSize(
                needsAreaSize,
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
        return Arrays.stream(new Direction.Axis[]{
            Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z
        }).map(
            axis -> NetherPortalMatcher.detectFrameFromInnerPos(
                fromWorld,
                firePos,
                axis,
                whatever -> true
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
    
    private static BlockPos getPosInOtherDimensionWithRandomShift(
        BlockPos pos,
        DimensionType dimensionFrom,
        DimensionType dimensionTo
    ) {
        
        Random rand = Helper.getServer().getWorld(dimensionFrom).random;
        BlockPos randomShift = new BlockPos(
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor
        );
        
        if (dimensionFrom == DimensionType.OVERWORLD && dimensionTo == DimensionType.THE_NETHER) {
            return new BlockPos(
                pos.getX() / 8,
                pos.getY() / 2,
                pos.getZ() / 8
            ).add(randomShift);
        }
        else if (dimensionFrom == DimensionType.THE_NETHER && dimensionTo == DimensionType.OVERWORLD) {
            return new BlockPos(
                pos.getX() * 8,
                pos.getY() * 2,
                pos.getZ() * 8
            ).add(randomShift);
        }
        else {
            return pos.add(randomShift);
        }
    }
    
    //it returns the primary id
    private static int registerPortalAndAllocateId(
        IWorld fromWorld,
        ObsidianFrame fromObsidianFrame,
        IWorld toWorld,
        ObsidianFrame toObsidianFrame
    ) {
        assert fromObsidianFrame.boxWithoutObsidian.getSize().equals(
            toObsidianFrame.boxWithoutObsidian.getSize()
        );
        
        assert fromObsidianFrame.normalAxis == toObsidianFrame.normalAxis;
        
        BlockPos innerAreaSize = fromObsidianFrame.boxWithoutObsidian.getSize();
        Direction.Axis normalAxis = fromObsidianFrame.normalAxis;
        
        Vec3d centerOffset = new Vec3d(innerAreaSize).scale(0.5);
        
        return PortalDataManager.getDataManagerOnServer().addFourPortals(
            fromWorld.getDimension().getType(),
            new Vec3d(fromObsidianFrame.boxWithoutObsidian.l)
                .add(centerOffset),
            toWorld.getDimension().getType(),
            new Vec3d(toObsidianFrame.boxWithoutObsidian.l)
                .add(centerOffset),
            normalAxis,
            new Vec3d(innerAreaSize)
        );
    }
    
    private static void generateObsidianFrame(
        IWorld world,
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
        IWorld world,
        ObsidianFrame obsidianFrame,
        int primaryPortalId
    ) {
        IntegerAABBInclusive contentBlockArea =
            obsidianFrame.boxWithoutObsidian;
        
        contentBlockArea.stream().forEach(
            blockPos -> setPortalContentBlock(
                world,
                blockPos,
                primaryPortalId,
                obsidianFrame.normalAxis
            )
        );
    }
    
    private static void setObsidian(
        IWorld world,
        BlockPos pos
    ) {
        boolean result = world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState(), 1 | 2);
    }
    
    private static void setPortalContentBlock(
        IWorld world,
        BlockPos pos,
        int primaryPortalId,
        Direction.Axis normalAxis
    ) {
        world.setBlockState(
            pos,
            BlockMyNetherPortal.instance.getDefaultState().with(
                BlockMyNetherPortal.AXIS, normalAxis
            ),
            3
        );
    }
    
}
