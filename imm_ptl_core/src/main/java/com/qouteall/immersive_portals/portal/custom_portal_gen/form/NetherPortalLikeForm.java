package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;

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
        ServerWorld fromWorld, BlockPos startingPos,
        ServerWorld toWorld
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
                fromWorld.setBlockState(areaPos, Blocks.AIR.getDefaultState());
            }
        }
        
        BlockPos toPos = cpg.mapPosition(fromShape.innerAreaBox.getCenter());
        
        Function<ChunkRegion, Function<BlockPos.Mutable, PortalGenInfo>> frameMatchingFunc =
            getFrameMatchingFunc(fromWorld, toWorld, fromShape);
        NetherPortalGeneration.startGeneratingPortal(
            fromWorld,
            toWorld,
            fromShape,
            toPos,
            128,
            otherSideFramePredicate,
            toShape -> {
                generateNewFrame(fromWorld, fromShape, toWorld, toShape);
            },
            info -> {
                //generate portal entity
                BreakablePortalEntity[] result = generatePortalEntitiesAndPlaceholder(info);
                for (BreakablePortalEntity portal : result) {
                    cpg.onPortalGenerated(portal);
                }
            },
            () -> {
                //place frame
                if (!generateFrameIfNotFound) {
                    return null;
                }
                
                BlockPortalShape toShape = getNewPortalPlacement(toWorld, toPos, fromShape);
                
                return toShape;
            },
            () -> {
                // check portal integrity while loading chunk
                return fromShape.frameAreaWithoutCorner.stream().allMatch(
                    bp -> !fromWorld.isAir(bp)
                );
            },
            frameMatchingFunc
        );
        
        return true;
    }
    
    public Function<ChunkRegion, Function<BlockPos.Mutable, PortalGenInfo>> getFrameMatchingFunc(
        ServerWorld fromWorld, ServerWorld toWorld,
        BlockPortalShape fromShape
    ) {
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        BlockPos.Mutable temp2 = new BlockPos.Mutable();
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
                        fromWorld.getRegistryKey(),
                        toWorld.getRegistryKey(),
                        fromShape, result
                    );
                }
            }
            return null;
        };
    }
    
    public BlockPortalShape getNewPortalPlacement(
        ServerWorld toWorld, BlockPos toPos,
        BlockPortalShape fromShape
    ) {
        IntBox airCubePlacement =
            NetherPortalGeneration.findAirCubePlacement(
                toWorld, toPos,
                fromShape.axis, fromShape.totalAreaBox.getSize()
            );
        
        return fromShape.getShapeWithMovedTotalAreaBox(
            airCubePlacement
        );
    }
    
    public BreakablePortalEntity[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        info.generatePlaceholderBlocks();
        return info.generateBiWayBiFacedPortal(GeneralBreakablePortal.entityType);
    }
    
    public abstract void generateNewFrame(
        ServerWorld fromWorld,
        BlockPortalShape fromShape,
        ServerWorld toWorld,
        BlockPortalShape toShape
    );
    
    public abstract Predicate<BlockState> getOtherSideFramePredicate();
    
    public abstract Predicate<BlockState> getThisSideFramePredicate();
    
    public abstract Predicate<BlockState> getAreaPredicate();
    
    public boolean testThisSideShape(ServerWorld fromWorld, BlockPortalShape fromShape) {
        return true;
    }
}
