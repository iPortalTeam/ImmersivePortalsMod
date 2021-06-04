package qouteall.imm_ptl.core.portal;

import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Random;

public class PortalPlaceholderBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.AXIS;
    public static final VoxelShape X_AABB = Block.createCuboidShape(
        6.0D,
        0.0D,
        0.0D,
        10.0D,
        16.0D,
        16.0D
    );
    public static final VoxelShape Y_AABB = Block.createCuboidShape(
        0.0D,
        6.0D,
        0.0D,
        16.0D,
        10.0D,
        16.0D
    );
    public static final VoxelShape Z_AABB = Block.createCuboidShape(
        0.0D,
        0.0D,
        6.0D,
        16.0D,
        16.0D,
        10.0D
    );
    
    public static PortalPlaceholderBlock instance;
    
    public PortalPlaceholderBlock(Settings properties) {
        super(properties);
        this.setDefaultState(
            (BlockState) ((BlockState) this.getStateManager().getDefaultState()).with(
                AXIS, Direction.Axis.X
            )
        );
    }
    
    @Override
    public VoxelShape getOutlineShape(
        BlockState state, BlockView world, BlockPos blockPos, ShapeContext shapeContext
    ) {
        switch ((Direction.Axis) state.get(AXIS)) {
            case Z:
                return Z_AABB;
            case Y:
                return Y_AABB;
            case X:
            default:
                return X_AABB;
        }
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(
        BlockState thisState,
        Direction direction,
        BlockState neighborState,
        WorldAccess worldAccess,
        BlockPos blockPos,
        BlockPos neighborPos
    ) {
        if (!worldAccess.isClient()) {
            if (worldAccess instanceof World) {
                World world = (World) worldAccess;
                
                world.getProfiler().push("portal_placeholder");
                
                Direction.Axis axis = thisState.get(AXIS);
                if (direction.getAxis() != axis) {
                    McHelper.findEntitiesRough(
                        BreakablePortalEntity.class,
                        world,
                        Vec3d.of(blockPos),
                        2,
                        e -> true
                    ).forEach(
                        portal -> {
                            ((BreakablePortalEntity) portal).notifyPlaceholderUpdate();
                        }
                    );
                }
                
                world.getProfiler().pop();
            }
        }
        
        return super.getStateForNeighborUpdate(
            thisState,
            direction,
            neighborState,
            worldAccess,
            blockPos,
            neighborPos
        );
    }
    
    //copied from PortalBlock
    @Override
    public void randomDisplayTick(
        BlockState blockState_1,
        World world_1,
        BlockPos blockPos_1,
        Random random_1
    ) {
        //nothing
    }
    
    
    //---------These are copied from BlockBarrier
    @Override
    public boolean isTranslucent(
        BlockState blockState_1,
        BlockView blockView_1,
        BlockPos blockPos_1
    ) {
        return true;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState blockState_1) {
        return BlockRenderType.INVISIBLE;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public float getAmbientOcclusionLightLevel(
        BlockState blockState_1,
        BlockView blockView_1,
        BlockPos blockPos_1
    ) {
        return 1.0F;
    }
    
}
