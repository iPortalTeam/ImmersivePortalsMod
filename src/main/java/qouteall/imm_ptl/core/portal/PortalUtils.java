package qouteall.imm_ptl.core.portal;

import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.my_util.RayTraceResult;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PortalUtils {
    /**
     * Note: It only considers portals. It does not consider blocks and other entities.
     * Note: Invisible portals are also considered in raytracing. For visible-only use the predicate.
     */
    @NotNull
    public static Optional<Pair<Portal, RayTraceResult>> raytracePortals(
        Level world, Vec3 from, Vec3 to,
        boolean includeGlobalPortal,
        Predicate<Portal> predicate
    ) {
        return lenientRayTracePortals(world, from, to, includeGlobalPortal, predicate, 0.001);
    }
    
    @NotNull
    public static Optional<Pair<Portal, RayTraceResult>> lenientRayTracePortals(
        Level world, Vec3 from, Vec3 to, boolean includeGlobalPortal,
        Predicate<Portal> predicate, double leniency
    ) {
        Stream<Portal> portalStream = McHelper.getEntitiesNearby(
            world,
            from,
            Portal.class,
            from.distanceTo(to)
        ).stream();
        if (includeGlobalPortal) {
            List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
            portalStream = Streams.concat(
                portalStream,
                globalPortals.stream()
            );
        }
        return portalStream.map(
            portal -> new Pair<Portal, RayTraceResult>(
                portal, portal.generalRayTrace(from, to, leniency)
            )
        ).filter(
            portalAndHitPos -> portalAndHitPos.getSecond() != null
                && predicate.test(portalAndHitPos.getFirst())
        ).min(
            Comparator.comparingDouble(
                portalAndHitPos -> portalAndHitPos.getSecond().hitPos().distanceToSqr(from)
            )
        );
    }
    
    public static Optional<Pair<Portal, RayTraceResult>> raytracePortalFromEntityView(
        Entity player, float tickDelta, double maxDistance, boolean includeGlobalPortal,
        Predicate<Portal> predicate
    ) {
        Vec3 from = player.getEyePosition(tickDelta);
        Vec3 to = from.add(player.getViewVector(tickDelta).scale(maxDistance));
        Level world = player.level();
        return raytracePortals(world, from, to, includeGlobalPortal, predicate);
    }
    
    public static record PortalAwareRaytraceResult(
        Level world,
        BlockHitResult hitResult,
        List<Portal> portalsPassingThrough
    ) {}
    
    @Nullable
    public static PortalAwareRaytraceResult portalAwareRayTrace(
        Entity entity, double maxDistance
    ) {
        return portalAwareRayTrace(
            entity.level(),
            entity.getEyePosition(),
            entity.getViewVector(1),
            maxDistance,
            entity
        );
    }
    
    @Nullable
    public static PortalAwareRaytraceResult portalAwareRayTrace(
        Level world,
        Vec3 startingPoint,
        Vec3 direction,
        double maxDistance,
        Entity entity
    ) {
        return portalAwareRayTrace(world, startingPoint, direction, maxDistance, entity, List.of());
    }
    
    @Nullable
    public static PortalAwareRaytraceResult portalAwareRayTrace(
        Level world,
        Vec3 startingPoint,
        Vec3 direction,
        double maxDistance,
        Entity entity,
        ClipContext.Block clipContextBlock
    ) {
        return portalAwareRayTrace(
            world, startingPoint, direction, maxDistance, entity,
            clipContextBlock, List.of()
        );
    }
    
    @Nullable
    public static PortalAwareRaytraceResult portalAwareRayTrace(
        Level world,
        Vec3 startingPoint,
        Vec3 direction,
        double maxDistance,
        Entity entity,
        @NotNull List<Portal> portalsPassingThrough
    ) {
        return portalAwareRayTrace(
            world, startingPoint, direction, maxDistance, entity,
            ClipContext.Block.OUTLINE, portalsPassingThrough
        );
    }
    
    @Nullable
    public static PortalAwareRaytraceResult portalAwareRayTrace(
        Level world, Vec3 startingPoint, Vec3 direction, double maxDistance,
        Entity entity, ClipContext.Block clipContextBlock,
        @NotNull List<Portal> portalsPassingThrough
    ) {
        if (portalsPassingThrough.size() > 5) {
            return null;
        }
        
        Vec3 endingPoint = startingPoint.add(direction.scale(maxDistance));
        Optional<Pair<Portal, RayTraceResult>> portalHit = raytracePortals(
            world, startingPoint, endingPoint, true,
            p -> {
                if (entity instanceof Player player) {
                    return p.isInteractableBy(player);
                }
                else {
                    return p.isVisible();
                }
            }
        );
        
        ClipContext context = new ClipContext(
            startingPoint,
            endingPoint,
            clipContextBlock,
            ClipContext.Fluid.NONE,
            entity
        );
        BlockHitResult blockHitResult = world.clip(context);
        
        boolean portalHitFound = portalHit.isPresent();
        boolean blockHitFound = blockHitResult.getType() == HitResult.Type.BLOCK;
        
        boolean shouldContinueRaytraceInsidePortal = false;
        if (portalHitFound && blockHitFound) {
            RayTraceResult rtResult = portalHit.get().getSecond();
            double portalDistance = rtResult.hitPos().distanceTo(startingPoint);
            double blockDistance = blockHitResult.getLocation().distanceTo(startingPoint);
            // add a little to block distance to make it raytrace to portal
            // when portal overlaps with block surface
            if (portalDistance < blockDistance + 0.0001) {
                // continue raytrace from within the portal
                shouldContinueRaytraceInsidePortal = true;
            }
            else {
                return new PortalAwareRaytraceResult(
                    world, blockHitResult, portalsPassingThrough
                );
            }
        }
        else if (!portalHitFound && blockHitFound) {
            return new PortalAwareRaytraceResult(
                world, blockHitResult, portalsPassingThrough
            );
        }
        else if (portalHitFound && !blockHitFound) {
            // continue raytrace from within the portal
            shouldContinueRaytraceInsidePortal = true;
        }
        
        if (shouldContinueRaytraceInsidePortal) {
            RayTraceResult rtResult = portalHit.get().getSecond();
            double portalDistance = rtResult.hitPos().distanceTo(startingPoint);
            Portal portal = portalHit.get().getFirst();
            
            Vec3 newStartingPoint = portal.transformPoint(rtResult.hitPos())
                .add(rtResult.surfaceNormal().scale(-0.001));
            Vec3 newDirection = portal.transformLocalVecNonScale(direction);
            double restDistance = maxDistance - portalDistance;
            if (restDistance < 0) {
                return null;
            }
            return portalAwareRayTrace(
                portal.getDestinationWorld(),
                newStartingPoint,
                newDirection,
                restDistance,
                entity,
                Stream.concat(
                    portalsPassingThrough.stream(), Stream.of(portal)
                ).collect(Collectors.toList())
            );
        }
        else {
            return null;
        }
    }
}
