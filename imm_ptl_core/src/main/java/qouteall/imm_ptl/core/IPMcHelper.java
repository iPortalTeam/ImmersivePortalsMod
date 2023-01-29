package qouteall.imm_ptl.core;

import com.mojang.blaze3d.platform.GlUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ducks.IERayTraceContext;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class IPMcHelper {
    public static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    // include global portals
    public static void foreachNearbyPortals(
        Level world, Vec3 pos, int range, Consumer<Portal> func
    ) {
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
        
        for (Portal globalPortal : globalPortals) {
            if (globalPortal.getDistanceToNearestPointInPortal(pos) < range * 2) {
                func.accept(globalPortal);
            }
        }
        
        McHelper.foreachEntitiesByPointAndRoughRadius(
            Portal.class, world, pos, range, func
        );
    }
    
    // include global portals
    public static List<Portal> getNearbyPortalList(
        Entity center, double range, Predicate<Portal> predicate
    ) {
        return getNearbyPortalList(center.level, center.position(), range, predicate);
    }
    
    // include global portals
    public static List<Portal> getNearbyPortalList(
        Level world, Vec3 pos, double range, Predicate<Portal> predicate
    ) {
        List<Portal> result = new ArrayList<>();
        foreachNearbyPortals(world, pos, (int) range, portal -> {
            if (predicate.test(portal)) {
                result.add(portal);
            }
        });
        return result;
    }
    
    // includes global portals
    public static Stream<Portal> getNearbyPortals(Entity center, double range) {
        return getNearbyPortals(center.level, center.position(), range);
    }
    
    // includes global portals
    public static Stream<Portal> getNearbyPortals(Level world, Vec3 pos, double range) {
        return getNearbyPortalList(world, pos, range, e -> true).stream();
    }
    
    public static void traverseNearbyPortals(
        Level world, Vec3 pos, int range, Consumer<Portal> func
    ) {
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
    
        for (Portal globalPortal : globalPortals) {
            if (globalPortal.getDistanceToNearestPointInPortal(pos) < range * 2) {
                func.accept(globalPortal);
            }
        }
        
        McHelper.traverseEntitiesByPointAndRoughRadius(
            Portal.class, world, pos, range, portal -> {
                func.accept(portal);
                return null;
            }
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
    public static List<Tuple<Portal, Vec3>> rayTracePortals(
        Level world,
        Vec3 start,
        Vec3 end,
        boolean includeGlobalPortals,
        Predicate<Portal> filter
    ) {
        // This will be the center of the chunk search, rather than using start or end. This will allow the radius to be
        // smaller, and as a result, the search to be faster and slightly less inefficient.
        //
        // The searching method employed by getNearbyEntities is still not ideal, but it's the best idea I have.
        Vec3 middle = start.scale(0.5).add(end.scale(0.5));
        
        // This could result in searching more chunks than necessary, but it always expands to completely cover any
        // chunks the line from start->end passes through.
        int chunkRadius = (int) Math.ceil(Math.abs(start.distanceTo(end) / 2) / 16);
        List<Portal> nearby = McHelper.getEntitiesNearby(world, middle, Portal.class, chunkRadius * 16);
        
        if (includeGlobalPortals) {
            nearby.addAll(GlobalPortalStorage.getGlobalPortals(world));
        }
        
        // Make a list of all portals actually intersecting with this line, and then sort them by the distance from the
        // start position. Nearest portals first.
        List<Tuple<Portal, Vec3>> hits = new ArrayList<>();
        
        nearby.forEach(portal -> {
            if (filter == null || filter.test(portal)) {
                Vec3 intersection = portal.rayTrace(start, end);
                
                if (intersection != null) {
                    hits.add(new Tuple<>(portal, intersection));
                }
            }
        });
        
        hits.sort((pair1, pair2) -> {
            Vec3 intersection1 = pair1.getB();
            Vec3 intersection2 = pair2.getB();
            
            // Return a negative number if intersection1 is smaller (should come first)
            return (int) Math.signum(intersection1.distanceToSqr(start) - intersection2.distanceToSqr(
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
    public static <T> T withSwitchedContext(Level world, Supplier<T> func) {
        if (world.isClientSide) {
            return ClientWorldLoader.withSwitchedWorld((ClientLevel) world, func);
        }
        else {
            return func.get();
        }
    }
    
    /**
     * @author LoganDark
     * @see IPMcHelper#rayTrace(World, RaycastContext, boolean, List)
     */
    private static Tuple<BlockHitResult, List<Portal>> rayTrace(
        Level world,
        ClipContext context,
        boolean includeGlobalPortals,
        List<Portal> portals
    ) {
        Vec3 start = context.getFrom();
        Vec3 end = context.getTo();
        
        // If we're past the max portal layer, don't let the player target behind this portal, create a missed result
        if (portals.size() > IPGlobal.maxPortalLayer) {
            Vec3 diff = end.subtract(start);
            
            return new Tuple<>(
                BlockHitResult.miss(
                    end,
                    Direction.getNearest(diff.x, diff.y, diff.z),
                    new BlockPos(end)
                ),
                portals
            );
        }
        
        // First ray trace normally
        BlockHitResult hitResult = world.clip(context);
        
        List<Tuple<Portal, Vec3>> rayTracedPortals =
            rayTracePortals(world, start, end, includeGlobalPortals, Portal::isInteractable);
        
        if (rayTracedPortals.isEmpty()) {
            return new Tuple<>(hitResult, portals);
        }
        
        Tuple<Portal, Vec3> portalHit = rayTracedPortals.get(0);
        Portal portal = portalHit.getA();
        Vec3 intersection = portalHit.getB();
        
        // If the portal is not closer, return the hit result we just got
        if (hitResult.getLocation().distanceToSqr(start) < intersection.distanceToSqr(start)) {
            return new Tuple<>(hitResult, portals);
        }
        
        // If the portal is closer, recurse
        
        IERayTraceContext betterContext = (IERayTraceContext) context;
        
        betterContext
            .setStart(portal.transformPoint(intersection))
            .setEnd(portal.transformPoint(end));
        
        portals.add(portal);
        Level destWorld = portal.getDestinationWorld();
        Tuple<BlockHitResult, List<Portal>> recursion = withSwitchedContext(
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
    public static Tuple<BlockHitResult, List<Portal>> rayTrace(
        Level world,
        ClipContext context,
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
    
    public static Component getDisableWarningText() {
        return Component.literal(" ").append(
            Component.translatable("imm_ptl.disable_warning").withStyle(
                style -> style.withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/imm_ptl_client_debug disable_warning"
                )).withUnderlined(true)
            ));
    }
    
    @Environment(EnvType.CLIENT)
    public static boolean isNvidiaVideocard() {
        return GlUtil.getVendor().toLowerCase().contains("nvidia");
    }
}
