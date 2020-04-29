package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BreakableMirror extends Mirror {
    
    public static EntityType<BreakableMirror> entityType;
    
    public IntBox wallArea;
    public boolean unbreakable = false;
    
    public BreakableMirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        wallArea = new IntBox(
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
        if (tag.contains("unbreakable")) {
            unbreakable = tag.getBoolean("unbreakable");
        }
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
        
        tag.putBoolean("unbreakable", unbreakable);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!world.isClient) {
            if (!unbreakable) {
                if (world.getTime() % 10 == getEntityId() % 10) {
                    checkWallIntegrity();
                }
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
        return world.getBlockState(blockPos).getBlock() == Blocks.GLASS;
        //return world.getBlockState(blockPos).getMaterial() == Material.GLASS;
    }
    
    public static BreakableMirror createMirror(
        ServerWorld world,
        BlockPos glassPos,
        Direction facing
    ) {
        if (!isGlass(world, glassPos)) {
            return null;
        }
        
        IntBox wallArea = Helper.expandRectangle(
            glassPos,
            blockPos -> isGlass(world, blockPos),
            facing.getAxis()
        );
        
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
        breakableMirror.updatePosition(
            pos.x, pos.y, pos.z
        );
        breakableMirror.destination = pos;
        breakableMirror.dimensionTo = world.dimension.getType();
        
        Pair<Direction.Axis, Direction.Axis> axises = Helper.getPerpendicularAxis(facing);
        
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
        
        breakIntersectedMirror(breakableMirror);
        
        return breakableMirror;
    }
    
    private static void breakIntersectedMirror(BreakableMirror newMirror) {
        McHelper.getEntitiesNearby(
            newMirror,
            BreakableMirror.class,
            20
        ).filter(
            mirror1 -> mirror1.getNormal().dotProduct(newMirror.getNormal()) > 0.5
        ).filter(
            mirror1 -> IntBox.getIntersect(
                mirror1.wallArea, newMirror.wallArea
            ) != null
        ).filter(
            mirror -> mirror != newMirror
        ).forEach(
            Entity::remove
        );
    }
}
