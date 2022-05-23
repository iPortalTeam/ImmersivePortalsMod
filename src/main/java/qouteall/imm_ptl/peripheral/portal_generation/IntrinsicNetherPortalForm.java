package qouteall.imm_ptl.peripheral.portal_generation;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.form.NetherPortalLikeForm;
import qouteall.imm_ptl.core.portal.custom_portal_gen.form.PortalGenForm;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;

import java.util.List;
import java.util.function.Predicate;

public class IntrinsicNetherPortalForm extends NetherPortalLikeForm {
    public IntrinsicNetherPortalForm() {
        super(true);
    }
    
    @Override
    public void generateNewFrame(ServerLevel fromWorld, BlockPortalShape fromShape, ServerLevel toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockAndUpdate(blockPos, Blocks.OBSIDIAN.defaultBlockState());
        }
    }
    
    @Override
    public PortalGenInfo getNewPortalPlacement(ServerLevel toWorld, BlockPos toPos, ServerLevel fromWorld, BlockPortalShape fromShape) {
        if (encounteredVanillaPortalBlock) {
            encounteredVanillaPortalBlock = false;
            if (IPGlobal.enableWarning) {
                List<ServerPlayer> nearbyPlayers = McHelper.findEntitiesRough(
                    ServerPlayer.class,
                    fromWorld,
                    Vec3.atLowerCornerOf(fromShape.anchor),
                    2,
                    p -> true
                );
                for (ServerPlayer player : nearbyPlayers) {
                    player.displayClientMessage(
                        new TranslatableComponent("imm_ptl.cannot_connect_to_vanilla_portal"),
                        false
                    );
                }
            }
        }
        
        return super.getNewPortalPlacement(toWorld, toPos, fromWorld, fromShape);
    }
    
    @Override
    public BreakablePortalEntity[] generatePortalEntitiesAndPlaceholder(PortalGenInfo info) {
        info.generatePlaceholderBlocks();
        BreakablePortalEntity[] portals = info.generateBiWayBiFacedPortal(NetherPortalEntity.entityType);
        
        return portals;
    }
    
    // not thread safe but mostly fine
    private static boolean encounteredVanillaPortalBlock = false;
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return blockState -> {
            if (O_O.isObsidian(blockState)) {
                return true;
            }
            Block block = blockState.getBlock();
            if (block == Blocks.NETHER_PORTAL) {
                encounteredVanillaPortalBlock = true;
            }
            return false;
        };
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return O_O::isObsidian;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return BlockBehaviour.BlockStateBase::isAir;
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
