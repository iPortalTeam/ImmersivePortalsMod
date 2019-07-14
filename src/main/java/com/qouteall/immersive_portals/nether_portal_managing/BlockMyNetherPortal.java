package com.qouteall.immersive_portals.nether_portal_managing;

import com.qouteall.immersive_portals.my_util.SignalBiArged;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;

public class BlockMyNetherPortal extends Block {
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
    
    public static final SignalBiArged<ServerWorld, BlockPos> portalBlockUpdateSignal = new SignalBiArged<>();
    
    public BlockMyNetherPortal(Settings properties) {
        super(properties);
        
        //this.setDefaultState(this.stateContainer.getBaseState().with(NEW_AXIS, EnumFacing.Axis.X));
    }
    
    public static final BlockMyNetherPortal instance =
        new BlockMyNetherPortal(
            FabricBlockSettings.of(Material.PORTAL)
                .noCollision()
                .sounds(BlockSoundGroup.GLASS)
                .strength(99999, 0)
                .lightLevel(11)
                .build()
        );
    
    @Override
    public VoxelShape getOutlineShape(
        BlockState blockState_1,
        BlockView blockView_1,
        BlockPos blockPos_1,
        EntityContext entityContext_1
    ) {
        switch ((Direction.Axis) blockState_1.get(AXIS)) {
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
    protected void appendProperties(StateFactory.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(
        BlockState blockState_1,
        Direction direction_1,
        BlockState blockState_2,
        IWorld iWorld_1,
        BlockPos blockPos_1,
        BlockPos blockPos_2
    ) {
        if (!iWorld_1.isClient()) {
            ServerWorld serverWorld = (ServerWorld) iWorld_1;
            portalBlockUpdateSignal.emit(serverWorld, blockPos_1);
        }
        return super.getStateForNeighborUpdate(
            blockState_1,
            direction_1,
            blockState_2,
            iWorld_1,
            blockPos_1,
            blockPos_2
        );
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
    
    @Override
    public boolean isOpaque(BlockState blockState_1) {
        return false;
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
    
    @Override
    public boolean allowsSpawning(
        BlockState blockState_1,
        BlockView blockView_1,
        BlockPos blockPos_1,
        EntityType<?> entityType_1
    ) {
        return false;
    }
    
}
