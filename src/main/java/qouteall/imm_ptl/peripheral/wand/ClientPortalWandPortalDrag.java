package qouteall.imm_ptl.peripheral.wand;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.my_util.Animated;
import qouteall.q_misc_util.my_util.RenderedPlane;

import java.util.EnumSet;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ClientPortalWandPortalDrag {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static enum PortalCorner {
        LEFT_BOTTOM, LEFT_TOP, RIGHT_BOTTOM, RIGHT_TOP;
        
        public int getXSign() {
            return switch (this) {
                case LEFT_BOTTOM, LEFT_TOP -> -1;
                case RIGHT_BOTTOM, RIGHT_TOP -> 1;
            };
        }
        
        public int getYSign() {
            return switch (this) {
                case LEFT_BOTTOM, RIGHT_BOTTOM -> -1;
                case LEFT_TOP, RIGHT_TOP -> 1;
            };
        }
        
        public Vec3 getOffset(Portal portal) {
            return portal.axisW.scale((portal.width / 2) * getXSign())
                .add(portal.axisH.scale((portal.height / 2) * getYSign()));
        }
    }
    
    @Nullable
    private static UUID targetPortalId;
    
    @Nullable
    private static final EnumSet<PortalCorner> lockedCorners = EnumSet.noneOf(PortalCorner.class);
    
    @Nullable
    private static PortalCorner draggingCorner;
    
    public static Animated<Vec3> cursor = new Animated<>(
        Animated.VEC_3_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.circle::mapProgress,
        null
    );
    
    public static Animated<RenderedPlane> renderedPlane = new Animated<>(
        Animated.RENDERED_PLANE_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        RenderedPlane.NONE
    );
    
    public static void updateDisplay() {
    
    }
}
