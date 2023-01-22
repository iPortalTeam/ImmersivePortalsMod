package qouteall.imm_ptl.core.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import javax.annotation.Nullable;

public class BreakableMirror extends Mirror {
    
    public static EntityType<BreakableMirror> entityType;
    
    @Nullable
    public IntBox wallArea;
    @Nullable
    public BlockPortalShape blockPortalShape;
    public boolean unbreakable = false;
    
    public BreakableMirror(EntityType<?> entityType_1, Level world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("boxXL")) {
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
        }
        else {
            wallArea = null;
        }
        if (tag.contains("blockPortalShape")) {
            blockPortalShape = BlockPortalShape.fromTag(tag.getCompound("blockPortalShape"));
        }
        else {
            blockPortalShape = null;
        }
        if (tag.contains("unbreakable")) {
            unbreakable = tag.getBoolean("unbreakable");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (wallArea != null) {
            tag.putInt("boxXL", wallArea.l.getX());
            tag.putInt("boxYL", wallArea.l.getY());
            tag.putInt("boxZL", wallArea.l.getZ());
            tag.putInt("boxXH", wallArea.h.getX());
            tag.putInt("boxYH", wallArea.h.getY());
            tag.putInt("boxZH", wallArea.h.getZ());
        }
        
        if (blockPortalShape != null) {
            tag.put("blockPortalShape", blockPortalShape.toTag());
        }
        
        tag.putBoolean("unbreakable", unbreakable);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            if (!unbreakable) {
                if (level.getGameTime() % 10 == getId() % 10) {
                    checkWallIntegrity();
                }
            }
        }
    }
    
    @Override
    public boolean isPortalValid() {
        return super.isPortalValid() && (wallArea != null || blockPortalShape != null);
    }
    
    private void checkWallIntegrity() {
        boolean wallValid;
        if (wallArea != null) {
            wallValid = wallArea.fastStream().allMatch(
                blockPos -> isGlass(level, blockPos)
            );
        }
        else if (blockPortalShape != null) {
            wallValid = blockPortalShape.area.stream().allMatch(
                blockPos -> isGlass(level, blockPos)
            );
        }
        else {
            wallValid = false;
        }
        if (!wallValid) {
            remove(RemovalReason.KILLED);
        }
    }
    
    public static boolean isGlass(Level world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof GlassBlock || block == Blocks.GLASS_PANE || block instanceof StainedGlassBlock;
    }
    
    private static boolean isGlassPane(Level world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block == Blocks.GLASS_PANE || block instanceof StainedGlassBlock;
    }
    
    public static BreakableMirror createMirror(
        ServerLevel world,
        BlockPos glassPos,
        Direction facing
    ) {
        if (!isGlass(world, glassPos)) {
            return null;
        }
        
        boolean isPane = isGlassPane(world, glassPos);
        
        // the top of stick-shape glass pane is too small. should not generate mirror there.
        if (facing.getAxis() == Direction.Axis.Y && isPane) {
            return null;
        }
        
        BlockPortalShape shape = BlockPortalShape.findArea(
            glassPos, facing.getAxis(),
            blockPos -> isGlass(world, blockPos) && (isPane == isGlassPane(world, blockPos)),
            blockPos -> !(isGlass(world, blockPos) && (isPane == isGlassPane(world, blockPos)))
        );
        
        if (shape == null) {
            return null;
        }
        
        BreakableMirror breakableMirror = BreakableMirror.entityType.create(world);
        assert breakableMirror != null;
        double distanceToCenter = isPane ? (1.0 / 16) : 0.5;
        
        AABB wallBox = McHelper.getWallBox(world, shape.area.stream());
        if (wallBox == null) {
            return null;
        }
        
        Vec3 pos = Helper.getBoxSurfaceInversed(wallBox, facing.getOpposite()).getCenter();
        pos = Helper.putCoordinate(
            //getWallBox is incorrect with corner glass pane so correct the coordinate on the normal axis
            pos, facing.getAxis(),
            Helper.getCoordinate(
                shape.innerAreaBox.getCenterVec().add(
                    Vec3.atLowerCornerOf(facing.getNormal()).scale(distanceToCenter)
                ),
                facing.getAxis()
            )
        );
        breakableMirror.setPos(pos.x, pos.y, pos.z);
        breakableMirror.setDestination(pos);
        breakableMirror.dimensionTo = world.dimension();
        
        shape.initPortalAxisShape(breakableMirror, pos, facing);
        
        breakableMirror.blockPortalShape = shape;
        
        breakIntersectedMirror(breakableMirror);
        
        world.addFreshEntity(breakableMirror);
        
        return breakableMirror;
    }
    
    public IntBox getAreaBox() {
        if (wallArea != null) {
            return wallArea;
        }
        else if (blockPortalShape != null) {
            return blockPortalShape.innerAreaBox;
        }
        else {
            throw new RuntimeException();
        }
    }
    
    private static void breakIntersectedMirror(BreakableMirror newMirror) {
        McHelper.getEntitiesNearby(
            newMirror,
            BreakableMirror.class,
            IPGlobal.maxNormalPortalRadius
        ).stream().filter(
            mirror1 -> mirror1.getNormal().dot(newMirror.getNormal()) > 0.5
        ).filter(
            mirror1 -> IntBox.getIntersect(
                mirror1.getAreaBox(), newMirror.getAreaBox()
            ) != null
        ).filter(
            mirror -> mirror != newMirror
        ).forEach(
            e -> e.remove(RemovalReason.KILLED)
        );
    }
    
}
