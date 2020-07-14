package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.custom_portal_gen.trigger.PortalGenTrigger;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class NetherPortalLikeForm extends PortalGenForm {
    public static final Codec<NetherPortalLikeForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.BLOCK.fieldOf("from_frame_block").forGetter(o -> o.fromFrameBlock),
            Registry.BLOCK.fieldOf("area_block").forGetter(o -> o.areaBlock),
            Registry.BLOCK.fieldOf("to_frame_block").forGetter(o -> o.toFrameBlock),
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound)
        ).apply(instance, instance.stable(NetherPortalLikeForm::new));
    });
    
    public final Block fromFrameBlock;
    public final Block areaBlock;
    public final Block toFrameBlock;
    public final boolean generateFrameIfNotFound;
    
    public NetherPortalLikeForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock, boolean generateFrameIfNotFound
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
        return new NetherPortalLikeForm(
            toFrameBlock,
            areaBlock,
            fromFrameBlock,
            generateFrameIfNotFound
        );
    }
    
    @Override
    public boolean perform(CustomPortalGeneration cpg, ServerWorld fromWorld, BlockPos startingPos) {
        //for example, using flint and stell will create a fire block
        //temporarily remove the fire and add it back
        BlockState startPosBlock = fromWorld.getBlockState(startingPos);
        fromWorld.setBlockState(startingPos, areaBlock.getDefaultState());
        
        Runnable finalizer = () -> fromWorld.setBlockState(startingPos, startPosBlock);
        
        Predicate<BlockState> areaPredicate = blockState -> blockState.getBlock() == areaBlock;
        Predicate<BlockState> thisSideFramePredicate = blockState -> blockState.getBlock() == fromFrameBlock;
        Predicate<BlockState> otherSideFramePredicate = blockState -> blockState.getBlock() == toFrameBlock;
        
        ServerWorld toWorld = McHelper.getServer().getWorld(cpg.toDimension);
        
        if (toWorld == null) {
            Helper.err("Cannot find dimension " + cpg.toDimension.getValue());
            finalizer.run();
            return false;
        }
        
        BlockPortalShape fromShape = NetherPortalGeneration.findFrameShape(
            fromWorld, startingPos,
            areaPredicate, thisSideFramePredicate
        );
        
        if (fromShape == null) {
            finalizer.run();
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
            thisSideFramePredicate,
            areaPredicate,
            otherSideFramePredicate,
            toShape -> {
                //generate new frame
                NetherPortalGeneration.embodyNewFrame(
                    toWorld,
                    toShape,
                    toFrameBlock.getDefaultState()
                );
            },
            info -> {
                //generate portal entity
                NetherPortalGeneration.generateBreakablePortalEntities(
                    info, GeneralBreakablePortal.entityType
                );
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
            }
        );
    
        return true;
    }
}
