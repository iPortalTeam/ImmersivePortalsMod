package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalMatcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class FloorSquareForm extends PortalGenForm {
    
    public static final ListCodec<Block> blockListCodec = new ListCodec<>(Registry.BLOCK);
    
    public static final Codec<FloorSquareForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Codec.INT.fieldOf("length").forGetter(o -> o.length),
            blockListCodec.fieldOf("frame_block").forGetter(o -> o.frameBlock),
            Registry.BLOCK.fieldOf("area_block").forGetter(o -> o.areaBlock),
            blockListCodec.fieldOf("up_frame_block").forGetter(o -> o.upFrameBlock)
        ).apply(instance, instance.stable(FloorSquareForm::new));
    });
    
    public int length;
    public List<Block> frameBlock;
    public Block areaBlock;
    public List<Block> upFrameBlock;
    
    public FloorSquareForm(int length, List<Block> frameBlock, Block areaBlock, List<Block> upFrameBlock) {
        this.length = length;
        this.frameBlock = frameBlock;
        this.areaBlock = areaBlock;
        this.upFrameBlock = upFrameBlock;
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
    public boolean perform(CustomPortalGeneration cpg, ServerWorld fromWorld, BlockPos startingPos) {
        Predicate<BlockState> areaPredicate = blockState -> blockState.getBlock() == areaBlock;
        Predicate<BlockState> framePredicate = blockState -> {
            return frameBlock.contains(blockState.getBlock());
        };
        Predicate<BlockState> otherSideFramePredicate = framePredicate;
        
        if (!areaPredicate.test(fromWorld.getBlockState(startingPos))) {
            return false;
        }
        
        ServerWorld toWorld = McHelper.getServer().getWorld(cpg.toDimension);
        
        if (toWorld == null) {
            Helper.err("Cannot find dimension " + cpg.toDimension.getValue());
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
                blockPos ->{
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
        boolean roughTest = areaSize.getX() == length &&
            areaSize.getZ() == length &&
            shape.area.size() == (length * length);
        if (!roughTest) {
            return false;
        }
        
        //empty indicates not checking up frame block
        if (upFrameBlock.isEmpty()) {
            return true;
        }
        
        Predicate<BlockState> upFrameBlockPredicate = s -> upFrameBlock.contains(s.getBlock());
        
        return shape.frameAreaWithCorner.stream().allMatch(
            blockPos -> upFrameBlockPredicate.test(fromWorld.getBlockState(blockPos))
        );
    }
}
