package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A portal's rendering related things
@Environment(EnvType.CLIENT)
public class PortalPresentation {
    
    // not stored inside Portal because this has its own dispose strategy
    private static final HashMap<Portal, PortalPresentation> dataMap = new HashMap<>();
    
    private static long lastPurgeTime = 0;
    
    // dispose by the last active time
    // using finalize() depends on GC and is not reliable
    private long lastActiveNanoTime;
    
    public static class Visibility {
        public GlQueryObject lastFrameQuery;
        public GlQueryObject thisFrameQuery;
        public Boolean lastFrameRendered;
        public Boolean thisFrameRendered;
        
        public Visibility() {
            lastFrameQuery = null;
            thisFrameQuery = null;
            lastFrameRendered = null;
        }
        
        void update() {
            if (lastFrameQuery != null) {
                GlQueryObject.returnQueryObject(lastFrameQuery);
            }
            lastFrameQuery = thisFrameQuery;
            thisFrameQuery = null;
            lastFrameRendered = thisFrameRendered;
            thisFrameRendered = null;
        }
        
        void dispose() {
            if (lastFrameQuery != null) {
                GlQueryObject.returnQueryObject(lastFrameQuery);
            }
            if (thisFrameQuery != null) {
                GlQueryObject.returnQueryObject(thisFrameQuery);
            }
        }
        
        GlQueryObject acquireThisFrameQuery() {
            if (thisFrameQuery == null) {
                thisFrameQuery = GlQueryObject.acquireQueryObject();
            }
            return thisFrameQuery;
        }
    }
    
    private final Map<List<UUID>, Visibility> infoMap = new HashMap<>();
    
    public int thisFrameQueryFrameIndex = -1;
    
    private long mispredictTime1 = 0;
    private long mispredictTime2 = 0;
    
    private int totalMispredictCount = 0;
    
    private boolean needsGroupingUpdate = true;
    @Nullable
    private PortalRenderingGroup renderingGroup;
    
    public static void init() {
        ModMain.preRenderSignal.connect(() -> {
            long currTime = System.nanoTime();
            if (currTime - lastPurgeTime > Helper.secondToNano(30)) {
                lastPurgeTime = currTime;
                dataMap.entrySet().removeIf(entry -> {
                    Portal portal = entry.getKey();
                    PortalPresentation presentation = entry.getValue();
                    
                    boolean shouldRemove = portal.removed || presentation.shouldDispose(currTime);
                    
                    if (shouldRemove) {
                        presentation.dispose();
                    }
                    
                    return shouldRemove;
                });
            }
        });
        
        Portal.clientPortalTickSignal.connect(portal -> {
            PortalPresentation presentation = getOptional(portal);
            if (presentation != null) {
                presentation.tick(portal);
            }
        });
        
        Portal.portalCacheUpdateSignal.connect(portal -> {
            if (portal.world.isClient()) {
                PortalPresentation presentation = getOptional(portal);
                if (presentation != null) {
                    presentation.onPortalCacheUpdate();
                }
            }
        });
    }
    
    public static void cleanup() {
        for (PortalPresentation presentation : dataMap.values()) {
            presentation.dispose();
        }
        dataMap.clear();
        
        Helper.log("Cleaning up portal presentation info");
    }
    
    private static void purge() {
        final long currTime = System.nanoTime();
        dataMap.entrySet().removeIf(entry -> {
            PortalPresentation presentation = entry.getValue();
            boolean shouldDispose = presentation.shouldDispose(currTime);
            if (shouldDispose) {
                presentation.dispose();
            }
            return shouldDispose;
        });
    }
    
    public static PortalPresentation get(Portal portal) {
        return dataMap.computeIfAbsent(
            portal, k -> new PortalPresentation()
        );
    }
    
    @Nullable
    public static PortalPresentation getOptional(Portal portal) {
        return dataMap.get(portal);
    }
    
    public PortalPresentation() {
        lastActiveNanoTime = System.nanoTime();
    }
    
    private void tick(Portal portal) {
        Validate.isTrue(portal.world.isClient());
        
        if (needsGroupingUpdate) {
            needsGroupingUpdate = false;
            updateGrouping(portal);
        }
    }
    
    private void onUsed() {
        lastActiveNanoTime = System.nanoTime();
    }
    
    private boolean shouldDispose(long currTime) {
        return currTime - lastActiveNanoTime > Helper.secondToNano(60);
    }
    
    private void dispose() {
        infoMap.values().forEach(Visibility::dispose);
        infoMap.clear();
    }
    
    // Visibility Predicting -----
    
    private void updateQuerySet() {
        onUsed();
        if (RenderStates.frameIndex != thisFrameQueryFrameIndex) {
            
            if (RenderStates.frameIndex == thisFrameQueryFrameIndex + 1) {
                infoMap.entrySet().removeIf(entry -> {
                    Visibility visibility = entry.getValue();
                    
                    return visibility.lastFrameQuery == null &&
                        visibility.thisFrameQuery == null;
                });
                
                infoMap.values().forEach(Visibility::update);
            }
            else {
                infoMap.values().forEach(Visibility::dispose);
                infoMap.clear();
            }
            
            thisFrameQueryFrameIndex = RenderStates.frameIndex;
        }
    }
    
    @Nonnull
    private Visibility getVisibility(List<UUID> desc) {
        updateQuerySet();
        
        return infoMap.computeIfAbsent(desc, k -> new Visibility());
    }
    
    private void onMispredict() {
        mispredictTime1 = mispredictTime2;
        mispredictTime2 = System.nanoTime();
        totalMispredictCount++;
    }
    
    private boolean isFrequentlyMispredicted() {
        if (totalMispredictCount > 5) {
            return true;
        }
        
        long currTime = System.nanoTime();
        
        return (currTime - mispredictTime1) < Helper.secondToNano(30);
    }
    
    private void updatePredictionStatus(Visibility visibility, boolean thisFrameDecision) {
        visibility.thisFrameRendered = thisFrameDecision;
        
        if (thisFrameDecision) {
            if (visibility.lastFrameRendered != null) {
                if (!visibility.lastFrameRendered) {
                    if (!isFrequentlyMispredicted()) {
                        onMispredict();
                    }
                }
            }
        }
    }
    
    public static boolean renderAndDecideVisibility(PortalLike portal, Runnable queryRendering) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        
        boolean decision;
        if (Global.offsetOcclusionQuery && portal instanceof Portal) {
            PortalPresentation presentation = get(((Portal) portal));
            
            List<UUID> renderingDescription = RenderInfo.getRenderingDescription();
            
            Visibility visibility = presentation.getVisibility(renderingDescription);
            
            GlQueryObject lastFrameQuery = visibility.lastFrameQuery;
            GlQueryObject thisFrameQuery = visibility.acquireThisFrameQuery();
            
            thisFrameQuery.performQueryAnySamplePassed(queryRendering);
            
            boolean noPredict =
                presentation.isFrequentlyMispredicted() ||
                    QueryManager.queryStallCounter <= 3;
            
            if (lastFrameQuery != null) {
                boolean lastFrameVisible = lastFrameQuery.fetchQueryResult();
                
                if (!lastFrameVisible && noPredict) {
                    profiler.push("fetch_this_frame");
                    decision = thisFrameQuery.fetchQueryResult();
                    profiler.pop();
                    QueryManager.queryStallCounter++;
                }
                else {
                    decision = lastFrameVisible;
                    presentation.updatePredictionStatus(visibility, decision);
                }
            }
            else {
                profiler.push("fetch_this_frame");
                decision = thisFrameQuery.fetchQueryResult();
                profiler.pop();
                QueryManager.queryStallCounter++;
            }
        }
        else {
            decision = QueryManager.renderAndGetDoesAnySamplePass(queryRendering);
        }
        return decision;
    }
    
    // Grouping -----
    
    private void onPortalCacheUpdate() {
        needsGroupingUpdate = true;
        renderingGroup = null;
    }
    
    private void setGroup(Portal portal, @Nullable PortalRenderingGroup group) {
        if (renderingGroup != null) {
            renderingGroup.removePortal(portal);
        }
        
        renderingGroup = group;
        if (renderingGroup != null) {
            renderingGroup.addPortal(portal);
        }
    }
    
    private void updateGrouping(Portal portal) {
        Validate.isTrue(!portal.isGlobalPortal);
        
        List<Portal> nearbyPortals = McHelper.findEntitiesByBox(
            Portal.class,
            portal.getOriginWorld(),
            portal.getBoundingBox().expand(1.5),
            10,
            p -> p != portal && !Portal.isFlippedPortal(p, portal)
        );
        
        Portal.TransformationDesc thisDesc = portal.getTransformationDesc();
        
        for (Portal nearbyPortal : nearbyPortals) {
            PortalPresentation nearbyPortalPresentation = get(nearbyPortal);
            
            PortalRenderingGroup itsGroup = nearbyPortalPresentation.renderingGroup;
            if (itsGroup != null) {
                if (itsGroup.transformationDesc.equals(thisDesc)) {
                    setGroup(portal, itsGroup);
                    return;
                }
            }
            else {
                Portal.TransformationDesc itsDesc = nearbyPortal.getTransformationDesc();
                if (thisDesc.equals(itsDesc)) {
                    PortalRenderingGroup newGroup = new PortalRenderingGroup(thisDesc);
                    setGroup(portal, newGroup);
                    get(nearbyPortal).setGroup(nearbyPortal, newGroup);
                    return;
                }
            }
            
        }
        
        setGroup(portal, null);
    }
    
    @Nullable
    public static PortalRenderingGroup getGroupOf(Portal portal) {
        Validate.isTrue(!portal.getIsGlobal());
        
        return get(portal).renderingGroup;
    }
    
}
