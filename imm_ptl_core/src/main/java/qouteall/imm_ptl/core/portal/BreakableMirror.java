package qouteall.imm_ptl.core.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

public class BreakableMirror extends Mirror {
    
    public static EntityType<BreakableMirror> entityType;
    
    public IntBox wallArea;
    public boolean unbreakable = false;
    
    public BreakableMirror(EntityType<?> entityType_1, Level world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
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
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
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
        return super.isPortalValid() && wallArea != null;
    }
    
    private void checkWallIntegrity() {
        boolean wallValid = wallArea.fastStream().allMatch(
            blockPos ->
                isGlass(level, blockPos)
        );
        if (!wallValid) {
            remove(RemovalReason.KILLED);
        }
    }
    
    public static boolean isGlass(Level world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof GlassBlock || block instanceof IronBarsBlock || block instanceof StainedGlassBlock;
    }
    
    private static boolean isGlassPane(Level world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof IronBarsBlock;
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
        
        AABB wallBox = getWallBox(world, wallArea);
        
        Vec3 pos = Helper.getBoxSurfaceInversed(wallBox, facing.getOpposite()).getCenter();
        pos = Helper.putCoordinate(
            //getWallBox is incorrect with corner glass pane so correct the coordinate on the normal axis
            pos, facing.getAxis(),
            Helper.getCoordinate(
                wallArea.getCenterVec().add(
                     Vec3.atLowerCornerOf(facing.getNormal()).scale(distanceToCenter)
                ),
                facing.getAxis()
            )
        );
        breakableMirror.setPos(pos.x, pos.y, pos.z);
        breakableMirror.setDestination(pos);
        breakableMirror.dimensionTo = world.dimension();
        
        Tuple<Direction, Direction> dirs =
            Helper.getPerpendicularDirections(facing);
        
        Vec3 boxSize = Helper.getBoxSize(wallBox);
        double width = Helper.getCoordinate(boxSize, dirs.getA().getAxis());
        double height = Helper.getCoordinate(boxSize, dirs.getB().getAxis());
        
        breakableMirror.axisW =  Vec3.atLowerCornerOf(dirs.getA().getNormal());
        breakableMirror.axisH =  Vec3.atLowerCornerOf(dirs.getB().getNormal());
        breakableMirror.width = width;
        breakableMirror.height = height;
        
        breakableMirror.wallArea = wallArea;
        
        breakIntersectedMirror(breakableMirror);
        
        world.addFreshEntity(breakableMirror);
        
        return breakableMirror;
    }
    
    private static void breakIntersectedMirror(BreakableMirror newMirror) {
        McHelper.getEntitiesNearby(
            newMirror,
            BreakableMirror.class,
            20
        ).stream().filter(
            mirror1 -> mirror1.getNormal().dot(newMirror.getNormal()) > 0.5
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
    private static AABB getWallBox(ServerLevel world, IntBox glassArea) {
        return glassArea.stream().map(blockPos ->
            world.getBlockState(blockPos).getCollisionShape(world, blockPos).bounds()
                .move( Vec3.atLowerCornerOf(blockPos))
        ).reduce(AABB::minmax).orElse(null);
    }
}
