package qouteall.imm_ptl.core.portal;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PortalUtils {
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
            entity.level,
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
        @NotNull List<Portal> portalsPassingThrough
    ) {
        if (portalsPassingThrough.size() > 5) {
            return null;
        }
        
        Vec3 endingPoint = startingPoint.add(direction.scale(maxDistance));
        Optional<Pair<Portal, Vec3>> portalHit = PortalManipulation.raytracePortals(
            world, startingPoint, endingPoint, true
        );
        
        ClipContext context = new ClipContext(
            startingPoint,
            endingPoint,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            entity
        );
        BlockHitResult blockHitResult = world.clip(context);
        
        boolean portalHitFound = portalHit.isPresent();
        boolean blockHitFound = blockHitResult.getType() == HitResult.Type.BLOCK;
        
        boolean shouldContinueRaytraceInsidePortal = false;
        if (portalHitFound && blockHitFound) {
            double portalDistance = portalHit.get().getSecond().distanceTo(startingPoint);
            double blockDistance = blockHitResult.getLocation().distanceTo(startingPoint);
            if (portalDistance < blockDistance) {
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
            double portalDistance = portalHit.get().getSecond().distanceTo(startingPoint);
            Portal portal = portalHit.get().getFirst();
            Vec3 newStartingPoint = portal.transformPoint(portalHit.get().getSecond())
                .add(portal.getContentDirection().scale(0.001));
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
