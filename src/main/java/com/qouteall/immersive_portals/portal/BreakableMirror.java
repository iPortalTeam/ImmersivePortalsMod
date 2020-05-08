package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
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
//        return world.getBlockState(blockPos).getBlock() == Blocks.GLASS;
        return world.getBlockState(blockPos).getMaterial() == Material.GLASS;
    }
    
    private static boolean isGlassPane(World world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return Registry.BLOCK.getId(block).toString().contains("pane");
    }
    
    public static BreakableMirror createMirror(
        ServerWorld world,
        BlockPos glassPos,
        Direction facing
    ) {
        if (!isGlass(world, glassPos)) {
            return null;
        }
        
        boolean isPane = isGlassPane(world, glassPos);

        if (facing.getAxis() == Direction.Axis.Y && isPane) {
            return null;
        }
        
        IntBox wallArea = Helper.expandRectangle(
            glassPos,
            blockPos -> isGlass(world, blockPos) && (isPane == isGlassPane(world, blockPos)),
            facing.getAxis()
        );
        
        BreakableMirror breakableMirror = BreakableMirror.entityType.create(world);
        double distanceToCenter = isPane ? (1.0 / 16) : 0.5;
        
        Box wallBox = getWallBox(world, wallArea);
        
        Vec3d pos = Helper.getBoxSurface(wallBox, facing.getOpposite()).getCenter();
        pos = Helper.putCoordinate(
            //getWallBox is incorrect with corner glass pane so correct the coordinate on the normal axis
            pos, facing.getAxis(),
            Helper.getCoordinate(
                wallArea.getCenterVec().add(
                    new Vec3d(facing.getVector()).multiply(distanceToCenter)
                ),
                facing.getAxis()
            )
        );
        breakableMirror.updatePosition(pos.x, pos.y, pos.z);
        breakableMirror.destination = pos;
        breakableMirror.dimensionTo = world.dimension.getType();
        
        Pair<Direction, Direction> dirs =
            Helper.getPerpendicularDirections(facing);
        
        Vec3d boxSize = Helper.getBoxSize(wallBox);
        double width = Helper.getCoordinate(boxSize, dirs.getLeft().getAxis());
        double height = Helper.getCoordinate(boxSize, dirs.getRight().getAxis());
        
        breakableMirror.axisW = new Vec3d(dirs.getLeft().getVector());
        breakableMirror.axisH = new Vec3d(dirs.getRight().getVector());
        breakableMirror.width = width;
        breakableMirror.height = height;
        
        breakableMirror.wallArea = wallArea;
        
        breakIntersectedMirror(breakableMirror);
        
        world.spawnEntity(breakableMirror);
        
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
    
    //it's a little bit incorrect with corner glass pane
    private static Box getWallBox(ServerWorld world, IntBox glassArea) {
        return glassArea.stream().map(blockPos ->
            world.getBlockState(blockPos).getCollisionShape(world, blockPos).getBoundingBox()
                .offset(new Vec3d(blockPos))
        ).reduce(Box::union).orElse(null);
    }
}
