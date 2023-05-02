package qouteall.imm_ptl.peripheral.portal_generation;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.Plane;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * WIP
 * The process and relevant marking rendering is handled purely on client side.
 * When it finishes, it performs a remote procedure call to create the portal.
 */
@Environment(EnvType.CLIENT)
public class PortalWandInteraction {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Nullable
    public static Vec3 cursorPointing;
    
    @Nullable
    public static ResourceKey<Level> firstSideDimension;
    // 3 points are enough for determining portal area
    @Nullable
    public static Vec3 firstSideLeftBottom;
    @Nullable
    public static Vec3 firstRightRightBottom;
    @Nullable
    public static Vec3 firstSideLeftUp;
    
    @Nullable
    public static Vec3 secondSideLeftBottom;
    @Nullable
    public static Vec3 secondSideRightBottom;
    @Nullable
    public static Vec3 secondSideLeftUp;
    
    public static void cancel() {
        firstSideLeftBottom = null;
        firstRightRightBottom = null;
        firstSideLeftUp = null;
        
        secondSideLeftBottom = null;
        secondSideRightBottom = null;
        secondSideLeftUp = null;
    }
    
    public static void onRightClick() {
        if (cursorPointing == null) {
            return;
        }
        
        if (firstSideLeftBottom == null) {
            firstSideLeftBottom = cursorPointing;
            return;
        }
        if (firstRightRightBottom == null) {
            firstRightRightBottom = cursorPointing;
            return;
        }
        if (firstSideLeftUp == null) {
            firstSideLeftUp = cursorPointing;
            return;
        }
        
        if (secondSideLeftBottom == null) {
            secondSideLeftBottom = cursorPointing;
            return;
        }
        if (secondSideRightBottom == null) {
            secondSideRightBottom = cursorPointing;
            return;
        }
        if (secondSideLeftUp == null) {
            secondSideLeftUp = cursorPointing;
            return;
        }
        
        finish();
    }
    
    private static void updateCursorPointingIfNecessary() {
        cursorPointing = null;
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        Vec3 viewVec = player.getViewVector(RenderStates.getPartialTick());
        
        HitResult hitResult = player.pick(100, RenderStates.getPartialTick(), false);
        
        if (hitResult.getType() == HitResult.Type.BLOCK && (hitResult instanceof BlockHitResult blockHitResult)) {
            cursorPointing = align(player.level, blockHitResult.getLocation());
        }
        
        Plane cursorLimitingPlane = getCursorLimitingPlane();
        if (cursorLimitingPlane != null) {
            // align it onto the plane
            if (cursorPointing != null && cursorLimitingPlane.getDistanceTo(cursorPointing) > 0.0001) {
                cursorPointing = cursorLimitingPlane.getProjection(cursorPointing);
            }
        }
    }
    
    private static Vec3 align(
        Level world, Vec3 vec3
    ) {
        BlockPos blockPos = BlockPos.containing(vec3);
        List<Vec3> relevantPoints = new IntBox(
            blockPos.offset(-1, -1, -1),
            blockPos.offset(1, 1, 1)
        ).stream().flatMap(
            pos -> {
                BlockState blockState = world.getBlockState(pos);
                VoxelShape collisionShape = blockState.getCollisionShape(world, pos);
                return collisionShape.toAabbs().stream();
            }
        ).flatMap(
            box -> Arrays.stream(Helper.eightVerticesOf(box))
        ).toList();
        
        relevantPoints = Helper.deduplicateWithPrecision(relevantPoints, 4096);
        
        return relevantPoints.stream().min(
            Comparator.comparingDouble(
                p -> p.distanceTo(vec3)
            )
        ).orElse(null);
    }
    
    // when the bottom left and bottom right are determined,
    // the left up point can only be in a plane because the portal is a rectangle
    @Nullable
    private static Plane getCursorLimitingPlane() {
        if (firstSideLeftBottom != null && firstRightRightBottom != null) {
            return new Plane(
                firstSideLeftBottom,
                firstRightRightBottom.subtract(firstSideLeftBottom).normalize()
            );
        }
        
        if (secondSideLeftBottom != null && secondSideRightBottom != null) {
            return new Plane(
                secondSideLeftBottom,
                secondSideRightBottom.subtract(secondSideLeftBottom).normalize()
            );
        }
        
        return null;
    }
    
    private static void finish() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
    
        if (firstSideLeftBottom == null || firstRightRightBottom == null || firstSideLeftUp == null ||
            secondSideLeftBottom == null || secondSideRightBottom == null || secondSideLeftUp == null
        ) {
            LOGGER.error("Portal wand interaction is not finished");
            return;
        }
        
        Vec3 firstSideHorizontalAxis = firstRightRightBottom.subtract(firstSideLeftBottom).normalize();
        Vec3 firstSideVerticalAxis = firstSideLeftUp.subtract(firstSideLeftBottom).normalize();
        double firstSideWidth = firstSideHorizontalAxis.length();
        double firstSideHeight = firstSideVerticalAxis.length();
    
        if (firstSideHorizontalAxis.dot(firstSideVerticalAxis) > 0.001) {
            LOGGER.error("The horizontal and vertical axis are not perpendicular in first side");
            return;
        }
        
        Vec3 secondSideHorizontalAxis = secondSideRightBottom.subtract(secondSideLeftBottom).normalize();
        Vec3 secondSideVerticalAxis = secondSideLeftUp.subtract(secondSideLeftBottom).normalize();
        double secondSideWidth = secondSideHorizontalAxis.length();
        double secondSideHeight = secondSideVerticalAxis.length();
        
        if (secondSideHorizontalAxis.dot(secondSideVerticalAxis) > 0.001) {
            LOGGER.error("The horizontal and vertical axis are not perpendicular in second side");
            return;
        }
    
        
        
    }
    
}
