package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Material;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class BreakableMirror extends Mirror {
    
    public static EntityType<BreakableMirror> entityType;
    
    public IntegerAABBInclusive wallArea;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "breakable_mirror"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<BreakableMirror> type, World world1) ->
                    new BreakableMirror(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
    }
    
    public BreakableMirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        wallArea = new IntegerAABBInclusive(
            new BlockPos(
                tag.getInt("boxXL"),
                tag.getInt("boxYL"),
                tag.getInt("boxZL")
            ),
            new BlockPos(
                tag.getInt("boxXH"),
                tag.getInt("boxYH"),
                tag.getInt("boxZH")
            )
        );
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);
        tag.putInt("boxXL", wallArea.l.getX());
        tag.putInt("boxYL", wallArea.l.getY());
        tag.putInt("boxZL", wallArea.l.getZ());
        tag.putInt("boxXH", wallArea.h.getX());
        tag.putInt("boxYH", wallArea.h.getY());
        tag.putInt("boxZH", wallArea.h.getZ());
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!world.isClient) {
            if (world.getTime() % 50 == getEntityId() % 50) {
                checkWallIntegrity();
            }
        }
    }
    
    @Override
    public boolean isPortalValid() {
        return super.isPortalValid() && wallArea != null;
    }
    
    private void checkWallIntegrity() {
        boolean wallValid = wallArea.fastStream().allMatch(
            blockPos ->
                isGlass(world, blockPos)
        );
        if (!wallValid) {
            removed = true;
        }
    }
    
    private static boolean isGlass(World world, BlockPos blockPos) {
        return world.getBlockState(blockPos).getMaterial() == Material.GLASS;
    }
    
    public static BreakableMirror createMirror(
        ServerWorld world,
        BlockPos glassPos,
        Direction facing
    ) {
        if (!isGlass(world, glassPos)) {
            return null;
        }
        
        IntegerAABBInclusive wallArea = new IntegerAABBInclusive(glassPos, glassPos);
        
        for (Direction direction : Helper.getAnotherFourDirections(facing.getAxis())) {
            wallArea = Helper.expandArea(
                wallArea,
                blockPos -> isGlass(world, blockPos),
                direction
            );
        }
        
        BreakableMirror breakableMirror = BreakableMirror.entityType.create(world);
        Vec3d pos = new Vec3d(
            (double) (wallArea.l.getX() + wallArea.h.getX()) / 2,
            (double) (wallArea.l.getY() + wallArea.h.getY()) / 2,
            (double) (wallArea.l.getZ() + wallArea.h.getZ()) / 2
        ).add(
            0.5, 0.5, 0.5
        ).add(
            new Vec3d(facing.getVector()).multiply(0.5)
        );
        breakableMirror.setPosition(
            pos.x, pos.y, pos.z
        );
        breakableMirror.destination = pos;
        breakableMirror.dimensionTo = world.dimension.getType();
        
        Pair<Direction.Axis, Direction.Axis> axises = Helper.getAnotherTwoAxis(facing.getAxis());
        if (facing.getDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Pair<>(axises.getRight(), axises.getLeft());
        }
        
        Direction.Axis wAxis = axises.getLeft();
        Direction.Axis hAxis = axises.getRight();
        float width = Helper.getCoordinate(wallArea.getSize(), wAxis);
        int height = Helper.getCoordinate(wallArea.getSize(), hAxis);
        
        breakableMirror.axisW = new Vec3d(
            Direction.get(Direction.AxisDirection.POSITIVE, wAxis).getVector()
        );
        breakableMirror.axisH = new Vec3d(
            Direction.get(Direction.AxisDirection.POSITIVE, hAxis).getVector()
        );
        breakableMirror.width = width;
        breakableMirror.height = height;
        
        breakableMirror.wallArea = wallArea;
        
        world.spawnEntity(breakableMirror);
        
        return breakableMirror;
    }
}
