package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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
        
        BlockPos toPos = cpg.mapPosition(fromShape.innerAreaBox.getCenter());
        
        NetherPortalGeneration.startGeneratingPortal(
            fromWorld,
            toWorld,
            fromShape,
            toPos,
            128,
            areaPredicate,
            otherSideFramePredicate,
            toShape -> {
                generateNewFrame(fromWorld, fromShape, toWorld, toShape);
            },
            info -> {
                //generate portal entity
                BreakablePortalEntity[] result = NetherPortalGeneration.generateBreakablePortalEntities(
                    info, GeneralBreakablePortal.entityType
                );
                for (BreakablePortalEntity portal : result) {
                    cpg.onPortalGenerated(portal);
                }
            },
            () -> {
                //place frame
                if (!generateFrameIfNotFound) {
                    return null;
                }
                
                IntBox airCubePlacement =
                    NetherPortalGeneration.findAirCubePlacement(
                        toWorld, toPos,
                        fromShape.axis, fromShape.totalAreaBox.getSize(),
                        128
                    );
                
                BlockPortalShape toShape = fromShape.getShapeWithMovedAnchor(
                    airCubePlacement.l.subtract(
                        fromShape.totalAreaBox.l
                    ).add(fromShape.anchor)
                );
                
                return toShape;
            },
            () -> {
                // check portal integrity while loading chunk
                // omitted
                return true;
            }
        );
        
        return true;
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
