package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.custom_portal_gen.SimpleBlockPredicate;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.annotation.Nullable;

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
    public BreakablePortalEntity[] generatePortalEntities(NetherPortalGeneration.Info info) {
        return FlippingFloorSquareForm.createPortals(
            McHelper.getServerWorld(info.from),
            McHelper.getServerWorld(info.to),
            info.fromShape, info.toShape
        );
    }
    
    @Nullable
    @Override
    public BlockPortalShape checkAndGetTemplateToShape(ServerWorld world, BlockPortalShape fromShape) {
        // only horizontal shape
        if (fromShape.axis != Direction.Axis.Y) {
            return null;
        }
        
        IntBox box = fromShape.innerAreaBox;
        BlockPos boxSize = box.getSize();
        // must be square
        if (boxSize.getX() != boxSize.getZ()) {
            return null;
        }
        if (boxSize.getX() * boxSize.getZ() != fromShape.area.size()) {
            return null;
        }
        
        return fromShape;
    }
    
    @Override
    protected BlockPortalShape getNewPortalPlacement(
        ServerWorld toWorld, BlockPos toPos,
        BlockPortalShape templateToShape
    ) {
        IntBox portalPlacement = FlippingFloorSquareForm.findPortalPlacement(
            toWorld,
            templateToShape.totalAreaBox.getSize(),
            toPos
        );
    
        return templateToShape.getShapeWithMovedTotalAreaBox(portalPlacement);
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
