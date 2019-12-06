package com.qouteall.immersive_portals.portal;

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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

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
    
    public static final SignalBiArged<ServerWorld, BlockPos> portalBlockUpdateSignal = new SignalBiArged<>();
    
    public static final PortalPlaceholderBlock instance =
        new PortalPlaceholderBlock(
            FabricBlockSettings.of(Material.PORTAL)
                .noCollision()
                .sounds(BlockSoundGroup.GLASS)
                .strength(99999, 0)
                .lightLevel(15)
                .build()
        );
    
    public static void init() {
        Registry.register(
            Registry.BLOCK,
            //the id is not appropriate because I did not implement end portal when making this block
            new Identifier("immersive_portals", "nether_portal_block"),
            instance
        );
    }
    
    public PortalPlaceholderBlock(Settings properties) {
        super(properties);
        this.setDefaultState(
            (BlockState) ((BlockState) this.stateFactory.getDefaultState()).with(
                AXIS, Direction.Axis.X
            )
        );
    }
    
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
    
    //copied from PortalBlock
    @Override
    public void randomDisplayTick(
        BlockState blockState_1,
        World world_1,
        BlockPos blockPos_1,
        Random random_1
    ) {
        if (random_1.nextInt(100) == 0) {
            world_1.playSound(
                (double) blockPos_1.getX() + 0.5D,
                (double) blockPos_1.getY() + 0.5D,
                (double) blockPos_1.getZ() + 0.5D,
                SoundEvents.BLOCK_PORTAL_AMBIENT,
                SoundCategory.BLOCKS,
                0.5F,
                random_1.nextFloat() * 0.4F + 0.8F,
                false
            );
        }
    
        for (int int_1 = 0; int_1 < 1; ++int_1) {
            double double_1 = (double) ((float) blockPos_1.getX() + random_1.nextFloat());
            double double_2 = (double) ((float) blockPos_1.getY() + random_1.nextFloat());
            double double_3 = (double) ((float) blockPos_1.getZ() + random_1.nextFloat());
            double double_4 = ((double) random_1.nextFloat() - 0.5D) * 0.5D;
            double double_5 = ((double) random_1.nextFloat() - 0.5D) * 0.5D;
            double double_6 = ((double) random_1.nextFloat() - 0.5D) * 0.5D;
            int int_2 = random_1.nextInt(2) * 2 - 1;
            if (world_1.getBlockState(blockPos_1.west()).getBlock() != this && world_1.getBlockState(
                blockPos_1.east()).getBlock() != this) {
                double_1 = (double) blockPos_1.getX() + 0.5D + 0.25D * (double) int_2;
                double_4 = (double) (random_1.nextFloat() * 2.0F * (float) int_2);
            }
            else {
                double_3 = (double) blockPos_1.getZ() + 0.5D + 0.25D * (double) int_2;
                double_6 = (double) (random_1.nextFloat() * 2.0F * (float) int_2);
            }
            
            world_1.addParticle(
                ParticleTypes.PORTAL,
                double_1,
                double_2,
                double_3,
                double_4,
                double_5,
                double_6
            );
        }
        
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
