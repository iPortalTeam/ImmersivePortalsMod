package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.function.Predicate;

public class DiligentForm extends PortalGenForm {
    public static final Codec<DiligentForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.BLOCK.fieldOf("from_frame_block").forGetter(o -> o.fromFrameBlock),
            Registry.BLOCK.fieldOf("area_block").forGetter(o -> o.areaBlock),
            Registry.BLOCK.fieldOf("to_frame_block").forGetter(o -> o.toFrameBlock),
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound)
        ).apply(instance, instance.stable(DiligentForm::new));
    });
    
    public final Block fromFrameBlock;
    public final Block areaBlock;
    public final Block toFrameBlock;
    public final boolean generateFrameIfNotFound;
    
    public DiligentForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock,
        boolean generateFrameIfNotFound
    ) {
        this.fromFrameBlock = fromFrameBlock;
        this.areaBlock = areaBlock;
        this.toFrameBlock = toFrameBlock;
        this.generateFrameIfNotFound = generateFrameIfNotFound;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return new DiligentForm(
            toFrameBlock, areaBlock, fromFrameBlock, generateFrameIfNotFound
        );
    }
    
    @Override
    public boolean perform(CustomPortalGeneration cpg, ServerWorld fromWorld, BlockPos startingPos, ServerWorld toWorld) {
        if (!NetherPortalGeneration.checkPortalGeneration(fromWorld, startingPos)) {
            return false;
        }
        
        Predicate<BlockState> areaPredicate = s -> s.getBlock() == areaBlock;
        Predicate<BlockState> thisSideFramePredicate = s -> s.getBlock() == fromFrameBlock;
        Predicate<BlockState> otherSideFramePredicate = s -> s.getBlock() == toFrameBlock;
        
        BlockPortalShape fromShape = NetherPortalGeneration.findFrameShape(
            fromWorld, startingPos,
            areaPredicate, thisSideFramePredicate
        );
        
        if (fromShape == null) {
            return false;
        }
        
        if (NetherPortalGeneration.isOtherGenerationRunning(
            fromWorld, fromShape.innerAreaBox.getCenterVec())
        ) {
            return false;
        }
        
        // clear the area
        if (generateFrameIfNotFound) {
            for (BlockPos areaPos : fromShape.area) {
                fromWorld.setBlockState(areaPos, Blocks.AIR.getDefaultState());
            }
        }
        
        BlockPos toPos = cpg.mapPosition(fromShape.innerAreaBox.getCenter());
        
        BlockPos.Mutable temp1 = new BlockPos.Mutable();
        
        NetherPortalGeneration.startGeneratingPortal(
            fromWorld,
            toWorld,
            fromShape,
            toPos,
            128,
            otherSideFramePredicate,
            toShape -> {
                for (BlockPos blockPos : toShape.frameAreaWithCorner) {
                    toWorld.setBlockState(blockPos, toFrameBlock.getDefaultState());
                }
            },
            info -> {
                //generate portal entity
                BreakablePortalEntity[] result =
                    info.createBiWayBiFacedPortal(GeneralBreakablePortal.entityType);
                for (BreakablePortalEntity portal : result) {
                    cpg.onPortalGenerated(portal);
                }
            },
            () -> {
                //place frame
                if (!generateFrameIfNotFound) {
                    return null;
                }
                
                BlockPortalShape toShape = NetherPortalLikeForm.getNewPortalPlacement(
                    toWorld, toPos, fromShape
                );
                
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
}
