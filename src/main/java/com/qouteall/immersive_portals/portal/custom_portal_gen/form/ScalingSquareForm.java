package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class ScalingSquareForm extends NetherPortalLikeForm {
    public static final Codec<ScalingSquareForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Registry.BLOCK.fieldOf("from_frame_block").forGetter(o -> o.fromFrameBlock),
            Registry.BLOCK.fieldOf("area_block").forGetter(o -> o.areaBlock),
            Registry.BLOCK.fieldOf("to_frame_block").forGetter(o -> o.toFrameBlock),
            Codec.INT.fieldOf("from_length").forGetter(o -> o.fromLength),
            Codec.INT.fieldOf("to_length").forGetter(o -> o.toLength),
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound)
        ).apply(instance, instance.stable(ScalingSquareForm::new));
    });
    
    public final Block fromFrameBlock;
    public final Block areaBlock;
    public final Block toFrameBlock;
    public final int fromLength;
    public final int toLength;
    
    public ScalingSquareForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock,
        int fromLength, int toLength, boolean generateFrameIfNotFound
    ) {
        super(generateFrameIfNotFound);
        this.fromFrameBlock = fromFrameBlock;
        this.areaBlock = areaBlock;
        this.toFrameBlock = toFrameBlock;
        this.fromLength = fromLength;
        this.toLength = toLength;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return new ScalingSquareForm(
            toFrameBlock, areaBlock, fromFrameBlock,
            toLength, fromLength, generateFrameIfNotFound
        );
    }
    
    @Nullable
    @Override
    public BlockPortalShape checkAndGetTemplateToShape(ServerWorld world, BlockPortalShape fromShape) {
        boolean isSquareShape = BlockPortalShape.isSquareShape(fromShape, fromLength);
        
        if (!isSquareShape) {
            return null;
        }
        
        return BlockPortalShape.getSquareShapeTemplate(
            fromShape.axis,
            toLength
        );
    }
    
    @Override
    public void generateNewFrame(ServerWorld fromWorld, BlockPortalShape fromShape, ServerWorld toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockState(blockPos, toFrameBlock.getDefaultState());
        }
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return s -> s.getBlock() == toFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return s -> s.getBlock() == fromFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return s -> s.getBlock() == areaBlock;
    }
    
    @Override
    public BreakablePortalEntity[] generatePortalEntities(NetherPortalGeneration.Info info) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(info.from);
        ServerWorld toWorld = McHelper.getServer().getWorld(info.to);
        
        NetherPortalGeneration.fillInPlaceHolderBlocks(fromWorld, info.fromShape);
        NetherPortalGeneration.fillInPlaceHolderBlocks(toWorld, info.toShape);
        
        EntityType<GeneralBreakablePortal> entityType = GeneralBreakablePortal.entityType;
        
        GeneralBreakablePortal f1 = entityType.create(fromWorld);
        info.fromShape.initPortalPosAxisShape(f1, false);
        f1.dimensionTo = info.to;
        f1.destination = info.toShape.innerAreaBox.getCenterVec();
        f1.scaling = ((double) toLength) / fromLength;
        
        GeneralBreakablePortal f2 = PortalManipulation.createFlippedPortal(f1, entityType);
        
        GeneralBreakablePortal t1 = PortalManipulation.createReversePortal(f1, entityType);
        GeneralBreakablePortal t2 = PortalManipulation.createFlippedPortal(t1, entityType);
        
        f1.blockPortalShape = info.fromShape;
        f2.blockPortalShape = info.fromShape;
        t1.blockPortalShape = info.toShape;
        t2.blockPortalShape = info.toShape;
        
        f1.reversePortalId = t1.getUuid();
        t1.reversePortalId = f1.getUuid();
        f2.reversePortalId = t2.getUuid();
        t2.reversePortalId = f2.getUuid();
    
        fromWorld.spawnEntity(f1);
        fromWorld.spawnEntity(f2);
        toWorld.spawnEntity(t1);
        toWorld.spawnEntity(t2);
        
        return new BreakablePortalEntity[]{f1, f2, t1, t2};
    }
}
