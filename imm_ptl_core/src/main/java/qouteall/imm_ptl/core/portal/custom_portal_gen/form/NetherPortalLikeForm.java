package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGeneration;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.my_util.IntBox;

import org.jetbrains.annotations.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class NetherPortalLikeForm extends PortalGenForm {
    public final boolean generateFrameIfNotFound;
    
    public NetherPortalLikeForm(boolean generateFrameIfNotFound) {
        this.generateFrameIfNotFound = generateFrameIfNotFound;
    }
    
    @Override
    public boolean perform(
        CustomPortalGeneration cpg,
        ServerLevel fromWorld, BlockPos startingPos,
        ServerLevel toWorld,
        @Nullable Entity triggeringEntity
    ) {
        if (!NetherPortalGeneration.checkPortalGeneration(fromWorld, startingPos)) {
            return false;
        }
        
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> thisSideFramePredicate = getThisSideFramePredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        
        BlockPortalShape fromShape = NetherPortalGeneration.findFrameShape(
            fromWorld, startingPos,
            areaPredicate, thisSideFramePredicate
        );
        
        if (fromShape == null) {
            return false;
        }
        
        if (!testThisSideShape(fromWorld, fromShape)) {
            return false;
        }
        
        if (NetherPortalGeneration.isOtherGenerationRunning(fromWorld, fromShape.innerAreaBox.getCenterVec())) {
            return false;
        }
        
        // clear the area
        if (generateFrameIfNotFound) {
            for (BlockPos areaPos : fromShape.area) {
                fromWorld.setBlockAndUpdate(areaPos, Blocks.AIR.defaultBlockState());
            }
        }
        
        BlockPos toPos = cpg.mapPosition(fromShape.innerAreaBox.getCenter(), fromWorld, toWorld);
        
        Function<WorldGenRegion, Function<BlockPos.MutableBlockPos, PortalGenInfo>> frameMatchingFunc =
            getFrameMatchingFunc(fromWorld, toWorld, fromShape);
        NetherPortalGeneration.startGeneratingPortal(
            fromWorld,
            toWorld,
            fromShape,
            toPos,
            IPGlobal.netherPortalFindingRadius,
            otherSideFramePredicate,
            toShape -> {
                generateNewFrame(fromWorld, fromShape, toWorld, toShape);
            },
            info -> {
                //generate portal entity
                Portal[] result = generatePortalEntitiesAndPlaceholder(info);
                
                cpg.onPortalsGenerated(result);
            },
            () -> {
                //place frame
                if (!generateFrameIfNotFound) {
                    return null;
                }
                
                return getNewPortalPlacement(toWorld, toPos, fromWorld, fromShape, triggeringEntity);
            },
            () -> {
                // check portal integrity while loading chunk
                return fromShape.frameAreaWithoutCorner.stream().allMatch(
                    bp -> !fromWorld.isEmptyBlock(bp)
                );
            },
            frameMatchingFunc
        );
        
        return true;
    }
    
    public Function<WorldGenRegion, Function<BlockPos.MutableBlockPos, PortalGenInfo>> getFrameMatchingFunc(
        ServerLevel fromWorld, ServerLevel toWorld,
        BlockPortalShape fromShape
    ) {
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        BlockPos.MutableBlockPos temp2 = new BlockPos.MutableBlockPos();
        return (region) -> (blockPos) -> {
            BlockPortalShape result = fromShape.matchShapeWithMovedFirstFramePos(
                pos -> areaPredicate.test(region.getBlockState(pos)),
                pos -> otherSideFramePredicate.test(region.getBlockState(pos)),
                blockPos,
                temp2
            );
            if (result != null) {
                if (fromWorld != toWorld || fromShape.anchor != result.anchor) {
                    return new PortalGenInfo(
                        fromWorld.dimension(),
                        toWorld.dimension(),
                        fromShape, result
                    );
                }
            }
            return null;
        };
    }
    
    @Nullable
    public PortalGenInfo getNewPortalPlacement(
        ServerLevel toWorld, BlockPos toPos,
        ServerLevel fromWorld, BlockPortalShape fromShape,
        @Nullable Entity triggeringEntity
    ) {
        boolean canForcePlace = false;
        if (triggeringEntity instanceof ServerPlayer player) {
            if (player.isCreative()) {
                canForcePlace = true;
            }
        }
        
        IntBox airCubePlacement =
            NetherPortalGeneration.findAirCubePlacement(
                toWorld, toPos,
                fromShape.axis, fromShape.totalAreaBox.getSize(),
                canForcePlace
            );
    
        if (!canForcePlace && airCubePlacement == null) {
            if (triggeringEntity instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("imm_ptl.no_place_to_generate_portal"),
                    false
                );
            }
        }
    
        if (airCubePlacement == null) {
            return null;
        }
        
        BlockPortalShape placedShape = fromShape.getShapeWithMovedTotalAreaBox(
            airCubePlacement
        );
        
        return new PortalGenInfo(
            fromWorld.dimension(),
            toWorld.dimension(),
            fromShape,
            placedShape
        );
    }
    
    public Portal[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        info.generatePlaceholderBlocks();
        return info.generateBiWayBiFacedPortal(GeneralBreakablePortal.entityType);
    }
    
    public abstract void generateNewFrame(
        ServerLevel fromWorld,
        BlockPortalShape fromShape,
        ServerLevel toWorld,
        BlockPortalShape toShape
    );
    
    public abstract Predicate<BlockState> getOtherSideFramePredicate();
    
    public abstract Predicate<BlockState> getThisSideFramePredicate();
    
    public abstract Predicate<BlockState> getAreaPredicate();
    
    public boolean testThisSideShape(ServerLevel fromWorld, BlockPortalShape fromShape) {
        return true;
    }
}
