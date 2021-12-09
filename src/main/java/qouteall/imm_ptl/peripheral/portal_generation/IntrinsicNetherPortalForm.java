package qouteall.imm_ptl.peripheral.portal_generation;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.form.NetherPortalLikeForm;
import qouteall.imm_ptl.core.portal.custom_portal_gen.form.PortalGenForm;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.function.Predicate;

public class IntrinsicNetherPortalForm extends NetherPortalLikeForm {
    public IntrinsicNetherPortalForm() {
        super(true);
    }
    
    @Override
    public void generateNewFrame(ServerWorld fromWorld, BlockPortalShape fromShape, ServerWorld toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState());
        }
    }
    
    public static void initializeOverlay(BreakablePortalEntity portal, BlockPortalShape shape) {
        Direction.Axis axis = shape.axis;
        if (axis == Direction.Axis.X) {
            portal.overlayOpacity = 0.5;
            portal.overlayBlockState = Blocks.NETHER_PORTAL.getDefaultState().with(
                NetherPortalBlock.AXIS,
                Direction.Axis.Z
            );
            portal.reloadAndSyncToClient();
        }
        else if (axis == Direction.Axis.Z) {
            portal.overlayOpacity = 0.5;
            portal.overlayBlockState = Blocks.NETHER_PORTAL.getDefaultState().with(
                NetherPortalBlock.AXIS,
                Direction.Axis.X
            );
            portal.reloadAndSyncToClient();
        }
    }
    
    @Override
    public PortalGenInfo getNewPortalPlacement(ServerWorld toWorld, BlockPos toPos, ServerWorld fromWorld, BlockPortalShape fromShape) {
        if (encounteredVanillaPortalBlock) {
            encounteredVanillaPortalBlock = false;
            if (IPGlobal.enableWarning) {
                List<ServerPlayerEntity> nearbyPlayers = McHelper.findEntitiesRough(
                    ServerPlayerEntity.class,
                    fromWorld,
                    Vec3d.of(fromShape.anchor),
                    2,
                    p -> true
                );
                for (ServerPlayerEntity player : nearbyPlayers) {
                    player.sendMessage(
                        new TranslatableText("imm_ptl.cannot_connect_to_vanilla_portal"),
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
        
        if (IPGlobal.netherPortalOverlay) {
            initializeOverlay(portals[0], info.fromShape);
            initializeOverlay(portals[1], info.fromShape);
            initializeOverlay(portals[2], info.toShape);
            initializeOverlay(portals[3], info.toShape);
        }
        
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
