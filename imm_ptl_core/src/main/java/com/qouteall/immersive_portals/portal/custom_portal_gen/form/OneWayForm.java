package com.qouteall.immersive_portals.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.api.PortalAPI;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGeneration;
import com.qouteall.immersive_portals.portal.custom_portal_gen.SimpleBlockPredicate;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

public class OneWayForm extends PortalGenForm {
    public static final Codec<OneWayForm> codec = RecordCodecBuilder.create(instance -> {
        return instance.group(
            SimpleBlockPredicate.codec.fieldOf("frame_block").forGetter(o -> o.frameBlock),
            SimpleBlockPredicate.codec.fieldOf("area_block").forGetter(o -> o.areaBlock),
            Codec.BOOL.fieldOf("bi_faced").forGetter(o -> o.biFaced)
        ).apply(instance, instance.stable(OneWayForm::new));
    });
    
    public final SimpleBlockPredicate frameBlock;
    public final SimpleBlockPredicate areaBlock;
    public final boolean biFaced;
    
    public OneWayForm(
        SimpleBlockPredicate frameBlock, SimpleBlockPredicate areaBlock, boolean biFaced
    ) {
        this.frameBlock = frameBlock;
        this.areaBlock = areaBlock;
        this.biFaced = biFaced;
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
        CustomPortalGeneration cpg, ServerWorld fromWorld,
        BlockPos startingPos, ServerWorld toWorld, @Nullable Entity triggeringEntity
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
            fromWorld.setBlockState(areaPos, Blocks.AIR.getDefaultState());
        }
        
        NetherPortalGeneration.fillInPlaceHolderBlocks(fromWorld, fromShape);
        
        GeneralBreakablePortal portal = GeneralBreakablePortal.entityType.create(fromWorld);
        Validate.notNull(portal);
        fromShape.initPortalPosAxisShape(portal, true);
        
        if (triggeringEntity == null) {
            portal.setDestination(portal.getOriginPos().add(0, 10, 0));
            portal.setDestinationDimension(fromWorld.getRegistryKey());
        }
        else {
            portal.setDestination(triggeringEntity.getCameraPosVec(1));
            portal.setDestinationDimension(triggeringEntity.world.getRegistryKey());
        }
        
        portal.blockPortalShape = fromShape;
        portal.markOneWay();
        McHelper.spawnServerEntity(portal);
        
        if (biFaced) {
            GeneralBreakablePortal flippedPortal = PortalAPI.createFlippedPortal(portal);
            flippedPortal.blockPortalShape = fromShape;
            flippedPortal.markOneWay();
            McHelper.spawnServerEntity(flippedPortal);
            
            cpg.onPortalsGenerated(new Portal[]{portal, flippedPortal});
        }
        else {
            cpg.onPortalsGenerated(new Portal[]{portal});
        }
        
        return true;
    }
}
