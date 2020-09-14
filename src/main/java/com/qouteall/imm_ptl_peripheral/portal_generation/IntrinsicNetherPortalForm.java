package com.qouteall.imm_ptl_peripheral.portal_generation;

import com.mojang.serialization.Codec;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.NetherPortalLikeForm;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.PortalGenForm;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalMatcher;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class IntrinsicNetherPortalForm extends NetherPortalLikeForm {
    public IntrinsicNetherPortalForm() {
        super(true);
    }
    
    public static boolean onFireLitOnObsidian(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        RegistryKey<World> fromDimension = fromWorld.getRegistryKey();
        
        RegistryKey<World> toDimension = NetherPortalGeneration.getDestinationDimension(fromDimension);
        
        if (toDimension == null) return false;
        
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        
        int searchingRadius = Global.netherPortalFindingRadius;
        
        BlockPortalShape thisSideShape = NetherPortalGeneration.triggerGeneratingPortal(
            fromWorld,
            firePos,
            toWorld,
            searchingRadius,
            (fromPos1) -> NetherPortalGeneration.mapPosition(
                fromPos1,
                fromWorld.getRegistryKey(),
                toWorld.getRegistryKey()
            ),
            //this side area
            NetherPortalMatcher::isAirOrFire,
            //this side frame
            O_O::isObsidian,
            //other side area
            BlockState::isAir,
            //other side frame
            O_O::isObsidian,
            (shape) -> NetherPortalGeneration.embodyNewFrame(toWorld, shape, Blocks.OBSIDIAN.getDefaultState()),
            info -> NetherPortalGeneration.generateBreakablePortalEntitiesAndPlaceholder(info, NetherPortalEntity.entityType)
        );
        return thisSideShape != null;
    }
    
    @Override
    public void generateNewFrame(ServerWorld fromWorld, BlockPortalShape fromShape, ServerWorld toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState());
        }
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
