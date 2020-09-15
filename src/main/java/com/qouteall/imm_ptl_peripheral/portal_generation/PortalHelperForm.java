package com.qouteall.imm_ptl_peripheral.portal_generation;

import com.mojang.serialization.Codec;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.custom_portal_gen.PortalGenInfo;
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
        for (BlockPos blockPos : toShape.frameAreaWithoutCorner) {
            toWorld.setBlockState(blockPos, ModMain.portalHelperBlock.getDefaultState());
        }
    }
    
    @Override
    public Portal[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        ServerWorld world = McHelper.getServerWorld(info.from);
        
        for (BlockPos blockPos : info.fromShape.area) {
            world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
        }
        
        world.setBlockState(info.fromShape.firstFramePos, Blocks.AIR.getDefaultState());
        world.setBlockState(info.toShape.firstFramePos, Blocks.AIR.getDefaultState());
        
        Portal portal = info.createTemplatePortal(Portal.entityType);
        Portal flipped = PortalManipulation.createFlippedPortal(portal, Portal.entityType);
        Portal reverse = PortalManipulation.createReversePortal(portal, Portal.entityType);
        Portal parallel = PortalManipulation.createReversePortal(flipped, Portal.entityType);
        
        Portal[] portals = {portal, flipped, reverse, parallel};
        
        for (Portal p : portals) {
            McHelper.spawnServerEntityToUnloadedArea(p);
        }
        
        return portals;
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
