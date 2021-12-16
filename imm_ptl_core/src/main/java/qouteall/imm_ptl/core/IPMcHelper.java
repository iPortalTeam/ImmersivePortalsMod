package qouteall.imm_ptl.core;

import com.google.common.collect.Streams;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.ducks.IERayTraceContext;
import qouteall.imm_ptl.core.network.IPCommonNetworkClient;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class IPMcHelper {
    // includes global portals
    public static Stream<Portal> getNearbyPortals(Entity center, double range) {
        return getNearbyPortals(center.world, center.getPos(), range);
    }
    
    // includes global portals
    public static Stream<Portal> getNearbyPortals(World world, Vec3d pos, double range) {
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
        
        Stream<Portal> nearbyPortals = McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
            world, pos, Portal.class, range
        );
        return Streams.concat(
            globalPortals.stream().filter(
                p -> p.getDistanceToNearestPointInPortal(pos) < range * 2
            ),
            nearbyPortals
        );
    }
    
    //avoid dedicated server crash
    public static void onClientEntityTick(Entity entity) {
        CrossPortalEntityRenderer.onEntityTickClient(entity);
    }
    
    /**
     * Returns all portals intersecting the line from start->end.
     *
     * @param world                The world in which to ray trace for portals.
     * @param start                The start of the line defining the ray to trace.
     * @param end                  The end of the line defining the ray to trace.
     * @param includeGlobalPortals Whether or not to include global portals in the ray trace.
     * @param filter               Filter the portals that this function returns. Nullable
     * @return A list of portals and their intersection points with the line, sorted by nearest portals first.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static List<Pair<Portal, Vec3d>> rayTracePortals(
        World world,
        Vec3d start,
        Vec3d end,
        boolean includeGlobalPortals,
        Predicate<Portal> filter
    ) {
        // This will be the center of the chunk search, rather than using start or end. This will allow the radius to be
        // smaller, and as a result, the search to be faster and slightly less inefficient.
        //
        // The searching method employed by getNearbyEntities is still not ideal, but it's the best idea I have.
        Vec3d middle = start.multiply(0.5).add(end.multiply(0.5));
        
        // This could result in searching more chunks than necessary, but it always expands to completely cover any
        // chunks the line from start->end passes through.
        int chunkRadius = (int) Math.ceil(Math.abs(start.distanceTo(end) / 2) / 16);
        List<Portal> nearby = McHelper.getEntitiesNearby(world, middle, Portal.class, chunkRadius * 16);
        
        if (includeGlobalPortals) {
            nearby.addAll(GlobalPortalStorage.getGlobalPortals(world));
        }
        
        // Make a list of all portals actually intersecting with this line, and then sort them by the distance from the
        // start position. Nearest portals first.
        List<Pair<Portal, Vec3d>> hits = new ArrayList<>();
        
        nearby.forEach(portal -> {
            if (filter == null || filter.test(portal)) {
                Vec3d intersection = portal.rayTrace(start, end);
                
                if (intersection != null) {
                    hits.add(new Pair<>(portal, intersection));
                }
            }
        });
        
        hits.sort((pair1, pair2) -> {
            Vec3d intersection1 = pair1.getRight();
            Vec3d intersection2 = pair2.getRight();
            
            // Return a negative number if intersection1 is smaller (should come first)
            return (int) Math.signum(intersection1.squaredDistanceTo(start) - intersection2.squaredDistanceTo(
                start));
        });
        
        return hits;
    }
    
    /**
     * Execute {@code func} with the world being set to {@code world}, hopefully bypassing any issues that may be
     * related to mutating a world that is not currently set as the current world.
     * <p>
     * You may safely nest this function within other context switches. It works on both the client and the server.
     *
     * @param world The world to switch the context to. The context will be restored when {@code func} is complete.
     * @param func  The function to execute while the context is switched.
     * @param <T>   The return type of {@code func}.
     * @return Whatever {@code func} returned.
     */
    public static <T> T withSwitchedContext(World world, Supplier<T> func) {
        if (world.isClient) {
            return IPCommonNetworkClient.withSwitchedWorld((ClientWorld) world, func);
        }
        else {
            return func.get();
        }
    }
    
    /**
     * @author LoganDark
     * @see IPMcHelper#rayTrace(World, RaycastContext, boolean, List)
     */
    private static Pair<BlockHitResult, List<Portal>> rayTrace(
        World world,
        RaycastContext context,
        boolean includeGlobalPortals,
        List<Portal> portals
    ) {
        Vec3d start = context.getStart();
        Vec3d end = context.getEnd();
        
        // If we're past the max portal layer, don't let the player target behind this portal, create a missed result
        if (portals.size() > IPGlobal.maxPortalLayer) {
            Vec3d diff = end.subtract(start);
            
            return new Pair<>(
                BlockHitResult.createMissed(
                    end,
                    Direction.getFacing(diff.x, diff.y, diff.z),
                    new BlockPos(end)
                ),
                portals
            );
        }
        
        // First ray trace normally
        BlockHitResult hitResult = world.raycast(context);
        
        List<Pair<Portal, Vec3d>> rayTracedPortals =
            rayTracePortals(world, start, end, includeGlobalPortals, Portal::isInteractable);
        
        if (rayTracedPortals.isEmpty()) {
            return new Pair<>(hitResult, portals);
        }
        
        Pair<Portal, Vec3d> portalHit = rayTracedPortals.get(0);
        Portal portal = portalHit.getLeft();
        Vec3d intersection = portalHit.getRight();
        
        // If the portal is not closer, return the hit result we just got
        if (hitResult.getPos().squaredDistanceTo(start) < intersection.squaredDistanceTo(start)) {
            return new Pair<>(hitResult, portals);
        }
        
        // If the portal is closer, recurse
        
        IERayTraceContext betterContext = (IERayTraceContext) context;
        
        betterContext
            .setStart(portal.transformPoint(intersection))
            .setEnd(portal.transformPoint(end));
        
        portals.add(portal);
        World destWorld = portal.getDestinationWorld();
        Pair<BlockHitResult, List<Portal>> recursion = withSwitchedContext(
            destWorld,
            () -> rayTrace(destWorld, context, includeGlobalPortals, portals)
        );
        
        betterContext
            .setStart(start)
            .setEnd(end);
        
        return recursion;
    }
    
    /**
     * Ray traces for blocks or whatever the {@code context} dictates.
     *
     * @param world                The world to ray trace in.
     * @param context              The ray tracing context to use. This context will be mutated as it goes but will be
     *                             returned back to normal before a result is returned to you, so you can act like it
     *                             hasn't been  mutated.
     * @param includeGlobalPortals Whether or not to include global portals in the ray trace. If this is false, then the
     *                             ray trace can pass right through them.
     * @return The BlockHitResult and the list of portals that we've passed through to get there. This list can be used
     * to transform looking directions or do whatever you want really.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static Pair<BlockHitResult, List<Portal>> rayTrace(
        World world,
        RaycastContext context,
        boolean includeGlobalPortals
    ) {
        return rayTrace(world, context, includeGlobalPortals, new ArrayList<>());
    }
    
    /**
     * @param hitResult The HitResult to check.
     * @return If the HitResult passed is either {@code null}, or of type {@link HitResult.Type#MISS}.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean hitResultIsMissedOrNull(HitResult hitResult) {
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }
}
