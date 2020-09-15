package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractDiligentForm extends NetherPortalLikeForm {
    public AbstractDiligentForm(boolean generateFrameIfNotFound) {
        super(generateFrameIfNotFound);
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
                    if (fromWorld != toWorld || !fromShape.anchor.equals(matched.anchor)) {
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
}
