package com.qouteall.imm_ptl_peripheral.portal_generation;

import com.mojang.serialization.Codec;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.AbstractDiligentForm;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.PortalGenForm;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class PortalHelperForm extends AbstractDiligentForm {
    public PortalHelperForm() {
        super(true);
    }
    
    @Override
    public void generateNewFrame(ServerWorld fromWorld, BlockPortalShape fromShape, ServerWorld toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : fromShape.frameAreaWithCorner) {
            fromWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState());
        }
        for (BlockPos blockPos : toShape.frameAreaWithoutCorner) {
            toWorld.setBlockState(blockPos, ModMain.portalHelperBlock.getDefaultState());
        }
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return blockState -> blockState.getBlock() == ModMain.portalHelperBlock;
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return blockState -> blockState.getBlock() == ModMain.portalHelperBlock;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return blockState -> blockState.isAir();
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
