package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import qouteall.imm_ptl.core.ProfilerCompat;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;

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
    
    public static final PortalPlaceholderBlock instance = new PortalPlaceholderBlock(
        BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(
                Registries.BLOCK,
                McHelper.newIdentifier("immersive_portals", "nether_portal_block")
            ))
            .noCollision()
            .sound(SoundType.GLASS)
            .strength(1.0f, 0)
            .noOcclusion()
            .noLootTable()
            .lightLevel((s) -> 15)
    );
    
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
    protected BlockState updateShape(
        BlockState thisState,
        LevelReader worldAccess,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos blockPos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (worldAccess instanceof Level world && !world.isClientSide()) {
            ProfilerCompat.push("portal_placeholder");
            
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
            
            ProfilerCompat.pop();
        }
        
        return super.updateShape(
            thisState,
            worldAccess,
            scheduledTickAccess,
            blockPos,
            direction,
            neighborPos,
            neighborState,
            random
        );
    }
    
    public static boolean isHitOnPlaceholder(HitResult hitResult, Level world) {
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            if (hitResult instanceof BlockHitResult blockHitResult) {
                Block hittingBlock = world.getBlockState(blockHitResult.getBlockPos()).getBlock();
                return hittingBlock == PortalPlaceholderBlock.instance;
            }
        }
        return false;
    }
    
    //---------These are copied from BlockBarrier
    @Override
    public boolean propagatesSkylightDown(BlockState blockState_1) {
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
