package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.custom_portal_gen.SimpleBlockPredicate;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.world.ServerWorld;
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
            Codec.INT.fieldOf("length").forGetter(o -> o.length),
            
            SimpleBlockPredicate.codec.fieldOf("frame_block").forGetter(o -> o.frameBlock),
            SimpleBlockPredicate.codec.fieldOf("area_block").forGetter(o -> o.areaBlock),
            SimpleBlockPredicate.codec.optionalFieldOf("up_frame_block", SimpleBlockPredicate.pass)
                .forGetter(o -> o.upFrameBlock),
            SimpleBlockPredicate.codec.optionalFieldOf("bottom_block", SimpleBlockPredicate.pass)
                .forGetter(o -> o.bottomBlock)
        
        ).apply(instance, instance.stable(FlippingFloorSquareForm::new));
    });
    
    public final int length;
    public final SimpleBlockPredicate frameBlock;
    public final SimpleBlockPredicate areaBlock;
    public final SimpleBlockPredicate upFrameBlock;
    public final SimpleBlockPredicate bottomBlock;
    
    public FlippingFloorSquareForm(
        int length,
        SimpleBlockPredicate frameBlock, SimpleBlockPredicate areaBlock,
        SimpleBlockPredicate upFrameBlock, SimpleBlockPredicate bottomBlock
    ) {
        this.length = length;
        this.frameBlock = frameBlock;
        this.areaBlock = areaBlock;
        this.upFrameBlock = upFrameBlock;
        this.bottomBlock = bottomBlock;
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
    public boolean perform(
        CustomPortalGeneration cpg,
        ServerWorld fromWorld, BlockPos startingPos,
        ServerWorld toWorld
    ) {
        Predicate<BlockState> areaPredicate = areaBlock;
        Predicate<BlockState> framePredicate = frameBlock;
        Predicate<BlockState> bottomPredicate = bottomBlock;
        
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
        
        if (!checkFromShape(fromShape, fromWorld)) {
            return false;
        }
        
        BlockPos areaSize = fromShape.innerAreaBox.getSize();
        
        BlockPos toPos = cpg.mapPosition(fromShape.innerAreaBox.l);
        
        IntBox placingBox = findPortalPlacement(toWorld, areaSize, toPos);
        
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
        
        createPortals(cpg, fromWorld, toWorld, fromShape, toShape);
        
        return true;
    }
    
    private IntBox findPortalPlacement(ServerWorld toWorld, BlockPos areaSize, BlockPos toPos) {
        return IntStream.range(toPos.getX() - 8, toPos.getX() + 8).boxed()
            .flatMap(x -> IntStream.range(toPos.getZ() - 8, toPos.getZ() + 8).boxed()
                .flatMap(z -> IntStream.range(5, toWorld.getDimensionHeight() - 5).map(
                    y -> toWorld.getDimensionHeight() - y
                ).mapToObj(y -> new BlockPos(x, y, z)))
            )
            .map(blockPos -> IntBox.getBoxByBasePointAndSize(areaSize, blockPos))
            .filter(intBox -> intBox.stream().allMatch(
                pos -> {
                    BlockState blockState = toWorld.getBlockState(pos);
                    return !blockState.isOpaqueFullCube(toWorld, pos) &&
                        blockState.getBlock() != PortalPlaceholderBlock.instance;
                }
            ))
            .filter(intBox -> intBox.getSurfaceLayer(Direction.DOWN)
                .getMoved(Direction.DOWN.getVector())
                .stream().allMatch(
                    blockPos -> {
                        BlockState blockState = toWorld.getBlockState(blockPos);
                        return blockState.isOpaqueFullCube(toWorld, blockPos) &&
                            blockState.getBlock() != PortalPlaceholderBlock.instance;
                    }
                )
            )
            .findFirst().orElseGet(() -> IntBox.getBoxByBasePointAndSize(areaSize, toPos))
            .getMoved(Direction.DOWN.getVector());
    }
    
    private void createPortals(
        CustomPortalGeneration cpg,
        ServerWorld fromWorld, ServerWorld toWorld,
        BlockPortalShape fromShape, BlockPortalShape toShape
    ) {
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
        
        pa.extension.motionAffinity = 0.1;
        pb.extension.motionAffinity = 0.1;
        
        pa.world.spawnEntity(pa);
        pb.world.spawnEntity(pb);
        
        cpg.onPortalGenerated(pa);
        cpg.onPortalGenerated(pb);
    }
    
    private boolean checkFromShape(BlockPortalShape shape, ServerWorld fromWorld) {
        BlockPos areaSize = shape.innerAreaBox.getSize();
        boolean areaSizeTest = areaSize.getX() == length &&
            areaSize.getZ() == length &&
            shape.area.size() == (length * length);
        if (!areaSizeTest) {
            return false;
        }
        
        return shape.frameAreaWithoutCorner.stream().allMatch(
            blockPos -> (upFrameBlock).test(fromWorld.getBlockState(blockPos.up()))
        ) && shape.area.stream().allMatch(
            blockPos -> (bottomBlock).test(fromWorld.getBlockState(blockPos.down()))
        );
    }
}
