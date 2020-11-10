package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.RenderingHierarchy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// A portal's rendering related things
@Environment(EnvType.CLIENT)
public class PortalPresentation {
    
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
    
    private static final WeakHashMap<Portal, PortalPresentation> objectMap =
        new WeakHashMap<>();
    
    public static void init() {
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
        
        Portal.portalDisposeSignal.connect(portal -> {
            if (portal.world.isClient()) {
                PortalPresentation presentation = getOptional(portal);
                if (presentation != null) {
                    presentation.dispose();
                    objectMap.remove(portal);
                }
            }
        });
    }
    
    @Nullable
    public static PortalPresentation getOptional(Portal portal) {
        Validate.isTrue(portal.world.isClient());
        
        return objectMap.get(portal);
    }
    
    public static PortalPresentation get(Portal portal) {
        Validate.isTrue(portal.world.isClient());
        
        return objectMap.computeIfAbsent(portal, k -> new PortalPresentation());
    }
    
    public PortalPresentation() {
    
    }
    
    private void tick(Portal portal) {
        Validate.isTrue(portal.world.isClient());
        
        if (needsGroupingUpdate) {
            needsGroupingUpdate = false;
            updateGrouping(portal);
        }
        
        if (renderingGroup != null) {
            renderingGroup.purge();
            if (renderingGroup.portals.size() <= 1) {
                setGroup(portal, null);
            }
        }
    }
    
    // disposing twice is fine
    public void dispose() {
        infoMap.values().forEach(Visibility::dispose);
        infoMap.clear();
        
    }
    
    // Visibility Predicting -----
    
    private void updateQuerySet() {
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
            
            List<UUID> renderingDescription = RenderingHierarchy.getRenderingDescription();
            
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
        
        if (!Global.mergePortalRendering) {
            return;
        }
        
        List<Portal> nearbyPortals = McHelper.findEntitiesByBox(
            Portal.class,
            portal.getOriginWorld(),
            portal.getBoundingBox().expand(0.5),
            portal.getSizeEstimation() * 2 + 5,
            p -> p != portal && !Portal.isFlippedPortal(p, portal)
        );
        
        Portal.TransformationDesc thisDesc = portal.getTransformationDesc();
        
        for (Portal that : nearbyPortals) {
            if (that instanceof Mirror) {
                continue;
            }
            
            PortalPresentation nearbyPortalPresentation = get(that);
            
            PortalRenderingGroup itsGroup = nearbyPortalPresentation.renderingGroup;
            if (itsGroup != null) {
                if (itsGroup.transformationDesc.equals(thisDesc)) {
                    if (renderingGroup == null) {
                        // this is not in group, put into its group
                        setGroup(portal, itsGroup);
                    }
                    else {
                        // this and that are both in group, merge
                        mergeGroup(renderingGroup, itsGroup);
                    }
                    return;
                }
            }
            else {
                Portal.TransformationDesc itsDesc = that.getTransformationDesc();
                if (thisDesc.equals(itsDesc)) {
                    if (renderingGroup == null) {
                        // this and that are not in any group
                        PortalRenderingGroup newGroup = new PortalRenderingGroup(thisDesc);
                        setGroup(portal, newGroup);
                        get(that).setGroup(that, newGroup);
                    }
                    else {
                        // this is in group and that is not in group
                        get(that).setGroup(that, renderingGroup);
                    }
                    
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
    
    private static void mergeGroup(PortalRenderingGroup g1, PortalRenderingGroup g2) {
        for (Portal portal : new ArrayList<>(g2.portals)) {
            get(portal).setGroup(portal, g1);
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        
        // normally if a portal is removed by calling remove() it will dispose normally
        // but that cannot be guaranteed
        // use this to avoid potential resource leak
        ModMain.clientTaskList.addTask(() -> {
            dispose();
            return true;
        });
    }
}
