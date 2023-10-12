package qouteall.imm_ptl.core.portal.nether_portal;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.TriIntPredicate;

/**
 * @param basePosX the base pos is in the frame
 */
public record FastBlockPortalShape(
    // the base pos is on the frame
    int basePosX, int basePosY, int basePosZ,
    Direction.Axis axis,
    // in local coordinate based on base pos
    int[] localAreaBlockCoords,
    // in local coordinate based on base pos
    // note the corner means outer corner
    int[] localFrameWithoutCornerBlockCoords,
    // in local coordinate based on base pos
    int[] localFrameCornerBlockCoords,
    // in world coordinate
    // this box does not contain the outer frame layer
    IntBox innerAreaBox,
    // in world coordinate
    IntBox totalAreaBox
) {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * as the points are on a plane, it compresses the 3D coord into 2D
     * the coord on axis is omitted. Other 2 coords are in the order of XYZ
     * if axis is X, XYZ -> YZ    AB <- WAB (W is the coord on axis)
     * if axis is Y, XYZ -> XZ    AB <- AWB
     * if axis is Z, XYZ -> XY    AB <- ABW
     */
    public static int toLocalA(Direction.Axis axis, int x, int y, int z) {
        return switch (axis) {
            case X -> y; case Y -> x; case Z -> x;
        };
    }
    
    public static int toLocalB(Direction.Axis axis, int x, int y, int z) {
        return switch (axis) {
            case X -> z; case Y -> z; case Z -> y;
        };
    }
    
    public static int toWorldX(Direction.Axis axis, int coordOnAxis, int a, int b) {
        return switch (axis) {
            case X -> coordOnAxis; case Y -> a; case Z -> a;
        };
    }
    
    public static int toWorldY(Direction.Axis axis, int coordOnAxis, int a, int b) {
        return switch (axis) {
            case X -> a; case Y -> coordOnAxis; case Z -> b;
        };
    }
    
    public static int toWorldZ(Direction.Axis axis, int coordOnAxis, int a, int b) {
        return switch (axis) {
            case X -> b; case Y -> b; case Z -> coordOnAxis;
        };
    }
    
    public static BlockPos toWorldBlockPos(Direction.Axis axis, int coordOnAxis, int a, int b) {
        return new BlockPos(
            toWorldX(axis, coordOnAxis, a, b),
            toWorldY(axis, coordOnAxis, a, b),
            toWorldZ(axis, coordOnAxis, a, b)
        );
    }
    
    // assemble two ints into one long, to avoid object allocation
    // (this is not needed when Valhalla is released)
    public static long assemble(int a, int b) {
        // this is not chunk pos but same as chunk pos
        return ChunkPos.asLong(a, b);
    }
    
    public static int getAFromAssemble(long l) {
        return ChunkPos.getX(l);
    }
    
    public static int getBFromAssemble(long l) {
        return ChunkPos.getZ(l);
    }
    
    public static FastBlockPortalShape create(
        Direction.Axis axis,
        int[] areaBlockCoords,
        int coordOnAxis
    ) {
        Validate.isTrue(areaBlockCoords.length % 2 == 0);
        Validate.isTrue(areaBlockCoords.length != 0);
        
        int areaBlockNum = areaBlockCoords.length / 2;
        
        LongOpenHashSet area = new LongOpenHashSet();
        for (int i = 0; i < areaBlockNum; i++) {
            int a = areaBlockCoords[i * 2];
            int b = areaBlockCoords[i * 2 + 1];
            area.add(assemble(a, b));
        }
        
        LongOpenHashSet frame = new LongOpenHashSet();
        LongOpenHashSet frameCorner = new LongOpenHashSet();
        
        // calculate the frame (without corner) positions
        for (int i = 0; i < areaBlockNum; i++) {
            // it's x coordinate from the coordinate of shape, not world x
            int a = areaBlockCoords[i * 2];
            int b = areaBlockCoords[i * 2 + 1];
            
            // if the adjacent pos is not on area, it's on frame
            if (!area.contains(assemble(a + 1, b))) frame.add(assemble(a + 1, b));
            
            if (!area.contains(assemble(a - 1, b))) frame.add(assemble(a - 1, b));
            
            if (!area.contains(assemble(a, b + 1))) frame.add(assemble(a, b + 1));
            
            if (!area.contains(assemble(a, b - 1))) frame.add(assemble(a, b - 1));
        }
        
        // calculate the frame corner positions
        for (int i = 0; i < areaBlockNum; i++) {
            int a = areaBlockCoords[i * 2];
            int b = areaBlockCoords[i * 2 + 1];
            
            // if the diagonal adjacent pos is not on area and not on frame,
            // it's on frame corner
            if (!area.contains(assemble(a + 1, b + 1)) &&
                !frame.contains(assemble(a + 1, b + 1))
            ) {
                frameCorner.add(assemble(a + 1, b + 1));
            }
            
            if (!area.contains(assemble(a + 1, b - 1)) &&
                !frame.contains(assemble(a + 1, b - 1))
            ) {
                frameCorner.add(assemble(a + 1, b - 1));
            }
            
            if (!area.contains(assemble(a - 1, b + 1)) &&
                !frame.contains(assemble(a - 1, b + 1))
            ) {
                frameCorner.add(assemble(a - 1, b + 1));
            }
            
            if (!area.contains(assemble(a - 1, b - 1)) &&
                !frame.contains(assemble(a - 1, b - 1))
            ) {
                frameCorner.add(assemble(a - 1, b - 1));
            }
        }
        
        // calculate the bounds
        int minA = areaBlockCoords[0];
        int maxA = areaBlockCoords[0];
        int minB = areaBlockCoords[1];
        int maxB = areaBlockCoords[1];
        for (int i = 1; i < areaBlockNum; i++) {
            int a = areaBlockCoords[i * 2];
            int b = areaBlockCoords[i * 2 + 1];
            
            if (a < minA) minA = a;
            if (a > maxA) maxA = a;
            if (b < minB) minB = b;
            if (b > maxB) maxB = b;
        }
        
        IntBox innerAreaBox = new IntBox(
            toWorldBlockPos(axis, coordOnAxis, minA, minB),
            toWorldBlockPos(axis, coordOnAxis, maxA, maxB)
        );
        
        IntBox totalAreaBox = new IntBox(
            toWorldBlockPos(axis, coordOnAxis, minA - 1, minB - 1),
            toWorldBlockPos(axis, coordOnAxis, maxA + 1, maxB + 1)
        );
        
        long firstFramePosAssemble = frame.iterator().next();
        int baseA = getAFromAssemble(firstFramePosAssemble);
        int baseB = getBFromAssemble(firstFramePosAssemble);
        BlockPos firstFramePos = toWorldBlockPos(axis, coordOnAxis, baseA, baseB);
        
        // subtract them by the base pos
        int[] localAreaBlockCoords = new int[areaBlockNum * 2];
        for (int i = 0; i < areaBlockNum; i++) {
            int a = areaBlockCoords[i * 2];
            int b = areaBlockCoords[i * 2 + 1];
            localAreaBlockCoords[i * 2] = a - baseA;
            localAreaBlockCoords[i * 2 + 1] = b - baseB;
        }
        
        int[] localFrameWithoutCornerBlockCoords = new int[frame.size() * 2];
        LongIterator framePosIter = frame.longIterator();
        for (int i = 0; i < frame.size(); i++) {
            long l = framePosIter.nextLong();
            int a = getAFromAssemble(l);
            int b = getBFromAssemble(l);
            localFrameWithoutCornerBlockCoords[i * 2] = a - baseA;
            localFrameWithoutCornerBlockCoords[i * 2 + 1] = b - baseB;
        }
        
        int[] localFrameCornerBlockCoords = new int[frameCorner.size() * 2];
        LongIterator frameCornerPosIter = frameCorner.longIterator();
        for (int i = 0; i < frameCorner.size(); i++) {
            long l = frameCornerPosIter.nextLong();
            int a = getAFromAssemble(l);
            int b = getBFromAssemble(l);
            localFrameCornerBlockCoords[i * 2] = a - baseA;
            localFrameCornerBlockCoords[i * 2 + 1] = b - baseB;
        }
        
        return new FastBlockPortalShape(
            firstFramePos.getX(), firstFramePos.getY(), firstFramePos.getZ(),
            axis,
            areaBlockCoords,
            localFrameWithoutCornerBlockCoords,
            localFrameCornerBlockCoords,
            innerAreaBox,
            totalAreaBox
        );
    }
    
    public static @Nullable FastBlockPortalShape fromTag(CompoundTag tag) {
        int axisInt = tag.getInt("axis");
        if (axisInt < 0 || axisInt > 2) {
            LOGGER.error("invalid axis {}", tag);
            return null;
        }
        
        Direction.Axis axis = Direction.Axis.values()[axisInt];
        
        ListTag positions = tag.getList("poses", Tag.TAG_INT);
        
        int numNum = positions.size();
        
        if (numNum == 0 || numNum % 3 != 0) {
            LOGGER.error("invalid poses tag {}", tag);
            return null;
        }
        
        int positionNum = numNum / 3;
        
        int firstPointX = positions.getInt(0);
        int firstPointY = positions.getInt(1);
        int firstPointZ = positions.getInt(2);
        
        IntArrayList areaBlockCoords = new IntArrayList();
        if (axis != Direction.Axis.X) areaBlockCoords.add(firstPointX);
        if (axis != Direction.Axis.Y) areaBlockCoords.add(firstPointY);
        if (axis != Direction.Axis.Z) areaBlockCoords.add(firstPointZ);
        
        int coordOnAxis = axis.choose(firstPointX, firstPointY, firstPointZ);
        for (int i = 1; i < positionNum; i++) {
            int x = positions.getInt(i * 3);
            int y = positions.getInt(i * 3 + 1);
            int z = positions.getInt(i * 3 + 2);
            
            if (axis.choose(x, y, z) != coordOnAxis) {
                LOGGER.error("invalid block position in {} at {}", tag, i);
                return null;
            }
            
            if (axis != Direction.Axis.X) areaBlockCoords.add(x);
            if (axis != Direction.Axis.Y) areaBlockCoords.add(y);
            if (axis != Direction.Axis.Z) areaBlockCoords.add(z);
        }
        
        return create(
            axis,
            areaBlockCoords.toIntArray(),
            coordOnAxis
        );
    }
    
    public CompoundTag toTag() {
        int coordOnAxis = axis.choose(basePosX, basePosY, basePosZ);
        
        CompoundTag tag = new CompoundTag();
        
        tag.putInt("axis", axis.ordinal());
        
        ListTag positions = new ListTag();
        
        positions.add(IntTag.valueOf(basePosX));
        positions.add(IntTag.valueOf(basePosY));
        positions.add(IntTag.valueOf(basePosZ));
        
        for (int i = 0; i < localAreaBlockCoords.length / 2; i++) {
            int a = localAreaBlockCoords[i * 2];
            int b = localAreaBlockCoords[i * 2 + 1];
            
            positions.add(IntTag.valueOf(toWorldX(axis, coordOnAxis, a, b)));
            positions.add(IntTag.valueOf(toWorldY(axis, coordOnAxis, a, b)));
            positions.add(IntTag.valueOf(toWorldZ(axis, coordOnAxis, a, b)));
        }
        
        tag.put("poses", positions);
        
        return tag;
    }
    
    public boolean matchShape(
        int newBaseX, int newBaseY, int newBaseZ,
        TriIntPredicate framePredicate,
        TriIntPredicate areaPredicate
    ) {
        for (int frameI = 0; frameI < localFrameWithoutCornerBlockCoords.length / 2; frameI++) {
            int a = localFrameWithoutCornerBlockCoords[frameI * 2];
            int b = localFrameWithoutCornerBlockCoords[frameI * 2 + 1];
            
            int x = toWorldX(axis, newBaseX, a, b);
            int y = toWorldY(axis, newBaseY, a, b);
            int z = toWorldZ(axis, newBaseZ, a, b);
            
            if (!framePredicate.test(x, y, z)) {
                return false;
            }
        }
        
        for (int areaI = 0; areaI < localAreaBlockCoords.length / 2; areaI++) {
            int a = localAreaBlockCoords[areaI * 2];
            int b = localAreaBlockCoords[areaI * 2 + 1];
            
            int x = toWorldX(axis, newBaseX, a, b);
            int y = toWorldY(axis, newBaseY, a, b);
            int z = toWorldZ(axis, newBaseZ, a, b);
            
            if (!areaPredicate.test(x, y, z)) {
                return false;
            }
        }
        
        return true;
    }
}
