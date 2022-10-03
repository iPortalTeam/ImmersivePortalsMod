package qouteall.imm_ptl.core.teleportation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import java.util.WeakHashMap;

public class AnimatedPortalTeleportationManager {
//
//    public final Portal portal;
//    private PortalState lastState;
//    private AABB lastPortalBoundingBox;
//    private long lastStateGameTime;
//    private double lastStateTickDelta;
//    private WeakHashMap<Entity, Vec3> entityToLastRelativePos;
//
//    public AnimatedPortalTeleportationManager(Portal portal) {
//        this.portal = portal;
//    }
//
//    private void updateServer() {
//        Validate.isTrue(!portal.level.isClientSide());
//
//
//        // on server side, check for teleporation of entities
//        McHelper.findEntitiesByBox(
//            Entity.class,
//            portal.level,
//            lastPortalBoundingBox.minmax(portal.getBoundingBox()),
//            2,
//            e->{
//                if (e instanceof Player) {
//                    return false;
//                }
//            }
//        );
//
//        lastState = portal.getPortalState();
//        lastPortalBoundingBox = portal.getBoundingBox();
//        lastStateGameTime = portal.level.getGameTime();
//        lastStateTickDelta = 0;
//    }
//
//    @Environment(EnvType.CLIENT)
//    private void updateClient() {
//        Validate.isTrue(portal.level.isClientSide());
//        lastState = portal.getPortalState();
//        lastPortalBoundingBox = portal.getBoundingBox();
//        lastStateGameTime = portal.level.getGameTime();
//        lastStateTickDelta = RenderStates.tickDelta;
//    }
}
