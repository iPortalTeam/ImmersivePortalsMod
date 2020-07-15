package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.Registry;

import java.util.function.Predicate;
import java.util.stream.IntStream;

public class FlippingFloorSquareForm extends PortalGenForm {
    
    public static final ListCodec<Block> blockListCodec = new ListCodec<>(Registry.BLOCK);
    
    public static final Codec<FlippingFloorSquareForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Identifier.CODEC.fieldOf("frame_block_tag").forGetter(o -> o.frameBlockTagId),
            Identifier.CODEC.fieldOf("area_block_tag").forGetter(o -> o.areaBlockTagId),
            Identifier.CODEC.optionalFieldOf("up_frame_block_tag", null)
                .forGetter(o -> o.upFrameBlockTagId),
            Identifier.CODEC.optionalFieldOf("bottom_block_tag", null)
                .forGetter(o -> o.bottomBlockTagId),
            Codec.INT.fieldOf("length").forGetter(o -> o.length)
        ).apply(instance, instance.stable(FlippingFloorSquareForm::new));
    });
    
    public Identifier frameBlockTagId;
    public Identifier areaBlockTagId;
    public Identifier upFrameBlockTagId;
    public Identifier bottomBlockTagId;
    
    public int length;
    public Tag<Block> frameBlockTag;
    public Tag<Block> areaBlockTag;
    public Tag<Block> upFrameBlockTag;
    public Tag<Block> bottomBlockTag;
    
    public FlippingFloorSquareForm(
        Identifier frameBlockTagId, Identifier areaBlockTagId,
        Identifier upFrameBlockTagId, Identifier bottomBlockTagId,
        int length
    ) {
        this.frameBlockTagId = frameBlockTagId;
        this.areaBlockTagId = areaBlockTagId;
        this.upFrameBlockTagId = upFrameBlockTagId;
        this.bottomBlockTagId = bottomBlockTagId;
        this.length = length;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return this;
    }
    
    @Override
    public boolean initAndCheck() {
        frameBlockTag = CustomPortalGenManagement.readBlockTag(frameBlockTagId);
        if (frameBlockTag == null) {
            return false;
        }
        
        areaBlockTag = CustomPortalGenManagement.readBlockTag(areaBlockTagId);
        if (areaBlockTag == null) {
            return false;
        }
        
        upFrameBlockTag = CustomPortalGenManagement.readBlockTag(upFrameBlockTagId);
        if (upFrameBlockTag == null) {
            return false;
        }
        
        bottomBlockTag = CustomPortalGenManagement.readBlockTag(bottomBlockTagId);
        if (bottomBlockTag == null) {
            return false;
        }
        
        return super.initAndCheck();
    }
    
    @Override
    public boolean perform(
        CustomPortalGeneration cpg,
        ServerWorld fromWorld, BlockPos startingPos,
        ServerWorld toWorld
    ) {
        Predicate<BlockState> areaPredicate = blockState -> blockState.isIn(areaBlockTag);
        Predicate<BlockState> framePredicate = blockState -> blockState.isIn(frameBlockTag);
        Predicate<BlockState> bottomPredicate = blockState -> blockState.isIn(bottomBlockTag);
        
        if (!areaPredicate.test(fromWorld.getBlockState(startingPos))) {
            return false;
        }
        
        if (!bottomPredicate.test(fromWorld.getBlockState(startingPos.down()))) {
            return false;
        }
        
        BlockPortalShape fromShape = BlockPortalShape.findArea(
            startingPos,
            Direction.Axis.Y,
            blockPos -> areaPredicate.test(fromWorld.getBlockState(blockPos)),
            blockPos -> framePredicate.test(fromWorld.getBlockState(blockPos))
        );
        
        if (fromShape == null) {
            return false;
        }
        
        if (!isFloorSquareShape(fromShape, fromWorld)) {
            return false;
        }
        
        BlockPos areaSize = fromShape.innerAreaBox.getSize();
        
        BlockPos toPos = cpg.mapPosition(fromShape.innerAreaBox.l);
        
        IntBox groundAirBox = Helper.getLastSatisfying(
            IntStream.range(0, toWorld.getDimensionHeight())
                .map(i -> toWorld.getDimensionHeight() - i)
                .mapToObj(y -> {
                    return IntBox.getBoxByBasePointAndSize(
                        areaSize,
                        new BlockPos(toPos.getX(), y, toPos.getZ())
                    );
                }),
            box -> box.stream().allMatch(
                blockPos -> {
                    BlockState blockState = toWorld.getBlockState(blockPos);
                    // regard plant block as air
                    return blockState.isAir() || blockState.getBlock() instanceof PlantBlock;
                }
            )
        );
        
        // find placement
        IntBox placingBox;
        if (groundAirBox != null) {
            placingBox = groundAirBox.getMoved(new BlockPos(0, -1, 0));
            
            boolean isIntersectingWithPortal =
                placingBox.stream().anyMatch(blockPos -> toWorld.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance);
            if (isIntersectingWithPortal) {
                int upShift = toWorld.getRandom().nextInt(5) + 3;
                placingBox = placingBox.getMoved(new BlockPos(0, upShift, 0));
            }
        }
        else {
            placingBox = IntBox.getBoxByBasePointAndSize(areaSize, toPos);
        }
        
        BlockPos offset = placingBox.l.subtract(fromShape.innerAreaBox.l);
        BlockPortalShape toShape = fromShape.getShapeWithMovedAnchor(
            fromShape.anchor.add(offset)
        );
        
        // clone the frame into the destination
        fromShape.frameAreaWithoutCorner.forEach(fromWorldPos -> {
            BlockPos toWorldPos = fromWorldPos.add(offset);
            toWorld.setBlockState(toWorldPos, fromWorld.getBlockState(fromWorldPos));
            toWorld.setBlockState(toWorldPos.up(), fromWorld.getBlockState(fromWorldPos.up()));
        });
        NetherPortalGeneration.fillInPlaceHolderBlocks(fromWorld, fromShape);
        NetherPortalGeneration.fillInPlaceHolderBlocks(toWorld, toShape);
        
        // create the portal
        GeneralBreakablePortal pa = GeneralBreakablePortal.entityType.create(fromWorld);
        fromShape.initPortalPosAxisShape(pa, true);
        
        pa.destination = toShape.innerAreaBox.getCenterVec();
        pa.dimensionTo = toWorld.getRegistryKey();
        pa.rotation = new Quaternion(
            new Vector3f(1, 0, 0),
            180,
            true
        );
        
        GeneralBreakablePortal pb = (GeneralBreakablePortal)
            PortalManipulation.createReversePortal(pa, GeneralBreakablePortal.entityType);
        
        pa.blockPortalShape = fromShape;
        pb.blockPortalShape = toShape;
        pa.reversePortalId = pb.getUuid();
        pb.reversePortalId = pa.getUuid();
        
        pa.motionAffinity = 0.1;
        pb.motionAffinity = 0.1;
        
        pa.world.spawnEntity(pa);
        pb.world.spawnEntity(pb);
        
        return true;
    }
    
    private boolean isFloorSquareShape(BlockPortalShape shape, ServerWorld fromWorld) {
        BlockPos areaSize = shape.innerAreaBox.getSize();
        boolean areaSizeTest = areaSize.getX() == length &&
            areaSize.getZ() == length &&
            shape.area.size() == (length * length);
        if (!areaSizeTest) {
            return false;
        }
        
        if (upFrameBlockTag == null) {
            return true;
        }
        
        Predicate<BlockState> upFrameBlockPredicate = s -> s.isIn(upFrameBlockTag);
        Predicate<BlockState> bottomPredicate = blockState -> blockState.isIn(bottomBlockTag);
        
        return shape.frameAreaWithCorner.stream().allMatch(
            blockPos -> upFrameBlockPredicate.test(fromWorld.getBlockState(blockPos))
        ) && shape.area.stream().allMatch(
            blockPos -> bottomPredicate.test(fromWorld.getBlockState(blockPos.down()))
        );
    }
}
