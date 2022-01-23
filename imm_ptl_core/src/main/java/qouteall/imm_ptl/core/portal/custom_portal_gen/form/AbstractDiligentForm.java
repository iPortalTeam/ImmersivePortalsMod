package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractDiligentForm extends NetherPortalLikeForm {
    public AbstractDiligentForm(boolean generateFrameIfNotFound) {
        super(generateFrameIfNotFound);
    }
    
    @Override
    public Function<WorldGenRegion, Function<BlockPos.MutableBlockPos, PortalGenInfo>> getFrameMatchingFunc(
        ServerLevel fromWorld, ServerLevel toWorld, BlockPortalShape fromShape
    ) {
        List<DiligentMatcher.TransformedShape> matchableShapeVariants =
            DiligentMatcher.getMatchableShapeVariants(fromShape, 20);
        
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        BlockPos.MutableBlockPos temp2 = new BlockPos.MutableBlockPos();
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
                            fromWorld.dimension(),
                            toWorld.dimension(),
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
