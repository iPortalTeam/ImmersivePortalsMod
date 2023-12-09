package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGeneration;
import qouteall.imm_ptl.core.portal.custom_portal_gen.SimpleBlockPredicate;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;

public class OneWayForm extends PortalGenForm {
    public static final Codec<OneWayForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            SimpleBlockPredicate.codec.fieldOf("frame_block").forGetter(o -> o.frameBlock),
            SimpleBlockPredicate.codec.fieldOf("area_block").forGetter(o -> o.areaBlock),
            Codec.BOOL.fieldOf("bi_faced").forGetter(o -> o.biFaced),
            Codec.BOOL.optionalFieldOf("breakable", true).forGetter(o -> o.breakable)
        ).apply(instance, instance.stable(OneWayForm::new));
    });
    
    public final SimpleBlockPredicate frameBlock;
    public final SimpleBlockPredicate areaBlock;
    public final boolean biFaced;
    public final boolean breakable;
    
    public OneWayForm(
        SimpleBlockPredicate frameBlock, SimpleBlockPredicate areaBlock,
        boolean biFaced, boolean breakable
    ) {
        this.frameBlock = frameBlock;
        this.areaBlock = areaBlock;
        this.biFaced = biFaced;
        this.breakable = breakable;
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
        CustomPortalGeneration cpg, ServerLevel fromWorld,
        BlockPos startingPos, ServerLevel toWorld, @Nullable Entity triggeringEntity
    ) {
        
        if (!NetherPortalGeneration.checkPortalGeneration(fromWorld, startingPos)) {
            return false;
        }
        
        BlockPortalShape fromShape = NetherPortalGeneration.findFrameShape(
            fromWorld, startingPos,
            areaBlock, frameBlock
        );
        
        if (fromShape == null) {
            return false;
        }
        
        // clear the area
        for (BlockPos areaPos : fromShape.area) {
            fromWorld.setBlockAndUpdate(areaPos, Blocks.AIR.defaultBlockState());
        }
        
        if (breakable) {
            NetherPortalGeneration.fillInPlaceHolderBlocks(fromWorld, fromShape);
        }
        
        GeneralBreakablePortal portal = GeneralBreakablePortal.entityType.create(fromWorld);
        Validate.notNull(portal);
        fromShape.initPortalPosAxisShape(portal, Direction.AxisDirection.POSITIVE);
        
        if (triggeringEntity == null) {
            portal.setDestination(portal.getOriginPos().add(0, 10, 0));
            portal.setDestinationDimension(fromWorld.dimension());
        }
        else {
            portal.setDestination(triggeringEntity.getEyePosition(1));
            portal.setDestinationDimension(triggeringEntity.level().dimension());
        }
        
        portal.blockPortalShape = fromShape;
        portal.markOneWay();
        McHelper.spawnServerEntity(portal);
        
        GeneralBreakablePortal[] resultPortals = null;
        
        if (biFaced) {
            GeneralBreakablePortal flippedPortal = PortalAPI.createFlippedPortal(portal);
            flippedPortal.blockPortalShape = fromShape;
            flippedPortal.markOneWay();
            McHelper.spawnServerEntity(flippedPortal);
            
            resultPortals = new GeneralBreakablePortal[]{portal, flippedPortal};
        }
        else {
            resultPortals = new GeneralBreakablePortal[]{portal};
        }
        
        if (!breakable) {
            for (GeneralBreakablePortal p : resultPortals) {
                p.unbreakable = true;
            }
        }
        
        cpg.onPortalsGenerated(resultPortals);
        
        return true;
    }
}
