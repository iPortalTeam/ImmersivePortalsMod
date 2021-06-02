package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Block;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
    protected void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
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
    protected void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
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
                if (world.getTime() % 10 == getId() % 10) {
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
            remove(RemovalReason.KILLED);
        }
    }
    
    public static boolean isGlass(World world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof GlassBlock || block instanceof PaneBlock || block instanceof StainedGlassBlock;
    }
    
    private static boolean isGlassPane(World world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof PaneBlock;
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
        
        Vec3d pos = Helper.getBoxSurfaceInversed(wallBox, facing.getOpposite()).getCenter();
        pos = Helper.putCoordinate(
            //getWallBox is incorrect with corner glass pane so correct the coordinate on the normal axis
            pos, facing.getAxis(),
            Helper.getCoordinate(
                wallArea.getCenterVec().add(
                     Vec3d.of(facing.getVector()).multiply(distanceToCenter)
                ),
                facing.getAxis()
            )
        );
        breakableMirror.setPosition(pos.x, pos.y, pos.z);
        breakableMirror.setDestination(pos);
        breakableMirror.dimensionTo = world.getRegistryKey();
        
        Pair<Direction, Direction> dirs =
            Helper.getPerpendicularDirections(facing);
        
        Vec3d boxSize = Helper.getBoxSize(wallBox);
        double width = Helper.getCoordinate(boxSize, dirs.getLeft().getAxis());
        double height = Helper.getCoordinate(boxSize, dirs.getRight().getAxis());
        
        breakableMirror.axisW =  Vec3d.of(dirs.getLeft().getVector());
        breakableMirror.axisH =  Vec3d.of(dirs.getRight().getVector());
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
            e->e.remove(RemovalReason.KILLED)
        );
    }
    
    //it's a little bit incorrect with corner glass pane
    private static Box getWallBox(ServerWorld world, IntBox glassArea) {
        return glassArea.stream().map(blockPos ->
            world.getBlockState(blockPos).getCollisionShape(world, blockPos).getBoundingBox()
                .offset( Vec3d.of(blockPos))
        ).reduce(Box::union).orElse(null);
    }
}
