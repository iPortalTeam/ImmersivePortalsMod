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

import javax.annotation.Nullable;
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
        
        BlockPortalShape templateToShape = checkAndGetTemplateToShape(fromWorld, fromShape);
        
        if (templateToShape == null) {
            return false;
        }
        
        BlockPos.Mutable temp1 = new BlockPos.Mutable();
        
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
                
                BlockPortalShape toShape = getNewPortalPlacement(toWorld, toPos, templateToShape);
                
                return toShape;
            },
            () -> {
                // check portal integrity while loading chunk
                return fromShape.frameAreaWithoutCorner.stream().allMatch(
                    bp -> !fromWorld.isAir(bp)
                );
            },
            //avoid linking to the beginning frame
            (region, blockPos) -> {
                BlockPortalShape result = templateToShape.matchShapeWithMovedFirstFramePos(
                    pos -> areaPredicate.test(region.getBlockState(pos)),
                    pos -> otherSideFramePredicate.test(region.getBlockState(pos)),
                    blockPos,
                    temp1
                );
                if (result != null) {
                    if (fromWorld != toWorld || fromShape.anchor != result.anchor) {
                        return result;
                    }
                }
                return null;
            }
        );
        
        return true;
    }
    
    protected BlockPortalShape getNewPortalPlacement(
        ServerWorld toWorld, BlockPos toPos,
        BlockPortalShape templateToShape
    ) {
        IntBox airCubePlacement =
            NetherPortalGeneration.findAirCubePlacement(
                toWorld, toPos,
                templateToShape.axis, templateToShape.totalAreaBox.getSize(),
                128
            );
        
        return templateToShape.getShapeWithMovedAnchor(
            airCubePlacement.l.subtract(
                templateToShape.totalAreaBox.l
            ).add(templateToShape.anchor)
        );
    }
    
    public BreakablePortalEntity[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        return NetherPortalGeneration.generateBreakablePortalEntitiesAndPlaceholder(
            info, GeneralBreakablePortal.entityType
        );
    }
    
    // if check fails, return null
    @Nullable
    public BlockPortalShape checkAndGetTemplateToShape(ServerWorld world, BlockPortalShape fromShape) {
        return fromShape;
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
}
