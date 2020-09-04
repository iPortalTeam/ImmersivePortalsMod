package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class DiligentForm extends NetherPortalLikeForm {
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
    
    public DiligentForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock,
        boolean generateFrameIfNotFound
    ) {
        super(generateFrameIfNotFound);
        this.fromFrameBlock = fromFrameBlock;
        this.areaBlock = areaBlock;
        this.toFrameBlock = toFrameBlock;
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
    public Function<ChunkRegion, Function<BlockPos.Mutable, PortalGenInfo>> getFrameMatchingFunc(
        ServerWorld fromWorld, ServerWorld toWorld, BlockPortalShape fromShape
    ) {
        List<DiligentMatcher.TransformedShape> matchableShapeVariants =
            DiligentMatcher.getMatchableShapeVariants(fromShape, 20);
        
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        BlockPos.Mutable temp2 = new BlockPos.Mutable();
        return (region) -> (blockPos) -> {
            for (DiligentMatcher.TransformedShape matchableShapeVariant : matchableShapeVariants) {
                BlockPortalShape template = matchableShapeVariant.transformedShape;
                BlockPortalShape matched = template.matchShapeWithMovedFirstFramePos(
                    pos -> areaPredicate.test(region.getBlockState(pos)),
                    pos -> otherSideFramePredicate.test(region.getBlockState(pos)),
                    blockPos,
                    temp2
                );
                if (matched != null) {
                    if (fromWorld != toWorld || fromShape.anchor != matched.anchor) {
                        return new PortalGenInfo(
                            fromWorld.getRegistryKey(),
                            toWorld.getRegistryKey(),
                            fromShape, matched,
                            matchableShapeVariant.rotation.toQuaternion(),
                            matchableShapeVariant.scale
                        );
                    }
                }
            }
            
            return null;
        };
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
}
