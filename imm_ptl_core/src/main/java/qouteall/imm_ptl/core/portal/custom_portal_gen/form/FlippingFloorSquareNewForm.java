package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.SimpleBlockPredicate;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;

import org.jetbrains.annotations.Nullable;

public class FlippingFloorSquareNewForm extends HeterogeneousForm {
    public static final Codec<FlippingFloorSquareNewForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound),
            SimpleBlockPredicate.codec.fieldOf("area_block").forGetter(o -> o.areaBlock),
            SimpleBlockPredicate.codec.fieldOf("frame_block").forGetter(o -> o.frameBlock)
        ).apply(instance, FlippingFloorSquareNewForm::new);
    });
    
    public FlippingFloorSquareNewForm(
        boolean generateFrameIfNotFound, SimpleBlockPredicate areaBlock,
        SimpleBlockPredicate frameBlock
    ) {
        super(generateFrameIfNotFound, areaBlock, frameBlock);
    }
    
    @Override
    public BreakablePortalEntity[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        ServerLevel fromWorld = McHelper.getServerWorld(info.from);
        ServerLevel toWorld = McHelper.getServerWorld(info.to);
        NetherPortalGeneration.fillInPlaceHolderBlocks(fromWorld, info.fromShape);
        NetherPortalGeneration.fillInPlaceHolderBlocks(toWorld, info.toShape);
        return FlippingFloorSquareForm.createPortals(
            fromWorld,
            toWorld,
            info.fromShape, info.toShape
        );
    }
    
    @Override
    public boolean testThisSideShape(ServerLevel fromWorld, BlockPortalShape fromShape) {
        // only horizontal shape
        if (fromShape.axis != Direction.Axis.Y) {
            return false;
        }
    
        IntBox box = fromShape.innerAreaBox;
        BlockPos boxSize = box.getSize();
        // must be square
        return boxSize.getX() == boxSize.getZ() &&
            boxSize.getX() * boxSize.getZ() == fromShape.area.size();
    }
    
    @Override
    public PortalGenInfo getNewPortalPlacement(
        ServerLevel toWorld, BlockPos toPos,
        ServerLevel fromWorld, BlockPortalShape fromShape,
        @Nullable Entity triggeringEntity
    ) {
        IntBox portalPlacement = FlippingFloorSquareForm.findPortalPlacement(
            toWorld,
            fromShape.totalAreaBox.getSize(),
            toPos
        );
    
        BlockPortalShape placedShape = fromShape.getShapeWithMovedTotalAreaBox(portalPlacement);
    
        return new PortalGenInfo(
            fromWorld.dimension(), toWorld.dimension(),
            fromShape, placedShape,
            DQuaternion.rotationByDegrees(
                new Vec3(1, 0, 0),
                180
            ), 1.0
        );
        
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        return codec;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return new FlippingFloorSquareNewForm(
            generateFrameIfNotFound,
            areaBlock, frameBlock
        );
    }
}
