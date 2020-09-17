package com.qouteall.imm_ptl_peripheral.portal_generation;

import com.mojang.serialization.Codec;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.AbstractDiligentForm;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.PortalGenForm;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class DiligentNetherPortalForm extends AbstractDiligentForm {
    public DiligentNetherPortalForm() {
        super(true);
    }
    
    @Override
    public void generateNewFrame(ServerWorld fromWorld, BlockPortalShape fromShape, ServerWorld toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState());
        }
    }
    
    @Override
    public BreakablePortalEntity[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        info.generatePlaceholderBlocks();
        return info.generateBiWayBiFacedPortal(NetherPortalEntity.entityType);
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return O_O::isObsidian;
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return O_O::isObsidian;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return AbstractBlock.AbstractBlockState::isAir;
    }
    
    @Override
    public Codec<? extends PortalGenForm> getCodec() {
        throw new RuntimeException();
    }
    
    @Override
    public PortalGenForm getReverse() {
        return this;
    }
}
