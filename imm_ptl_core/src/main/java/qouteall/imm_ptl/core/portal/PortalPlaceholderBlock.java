package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;

import java.util.Random;

public class PortalPlaceholderBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final VoxelShape X_AABB = Block.box(
        6.0D,
        0.0D,
        0.0D,
        10.0D,
        16.0D,
        16.0D
    );
    public static final VoxelShape Y_AABB = Block.box(
        0.0D,
        6.0D,
        0.0D,
        16.0D,
        10.0D,
        16.0D
    );
    public static final VoxelShape Z_AABB = Block.box(
        0.0D,
        0.0D,
        6.0D,
        16.0D,
        16.0D,
        10.0D
    );
    
    public static PortalPlaceholderBlock instance;
    
    public PortalPlaceholderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            (BlockState) ((BlockState) this.getStateDefinition().any()).setValue(
                AXIS, Direction.Axis.X
            )
        );
    }
    
    @Override
    public VoxelShape getShape(
        BlockState state, BlockGetter world, BlockPos blockPos, CollisionContext shapeContext
    ) {
        switch ((Direction.Axis) state.getValue(AXIS)) {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
    
    @Override
    public BlockState updateShape(
        BlockState thisState,
        Direction direction,
        BlockState neighborState,
        LevelAccessor worldAccess,
        BlockPos blockPos,
        BlockPos neighborPos
    ) {
        if (!worldAccess.isClientSide()) {
            if (worldAccess instanceof Level) {
                Level world = (Level) worldAccess;
                
                world.getProfiler().push("portal_placeholder");
                
                Direction.Axis axis = thisState.getValue(AXIS);
                if (direction.getAxis() != axis) {
                    McHelper.findEntitiesRough(
                        BreakablePortalEntity.class,
                        world,
                        Vec3.atLowerCornerOf(blockPos),
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
        
        return super.updateShape(
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
    public void animateTick(
        BlockState blockState_1,
        Level world_1,
        BlockPos blockPos_1,
        Random random_1
    ) {
        //nothing
    }
    
    
    //---------These are copied from BlockBarrier
    @Override
    public boolean propagatesSkylightDown(
        BlockState blockState_1,
        BlockGetter blockView_1,
        BlockPos blockPos_1
    ) {
        return true;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState blockState_1) {
        return RenderShape.INVISIBLE;
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public float getShadeBrightness(
        BlockState blockState_1,
        BlockGetter blockView_1,
        BlockPos blockPos_1
    ) {
        return 1.0F;
    }
    
}
