package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.render.GlQueryObject;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.imm_ptl.core.render.QueryManager;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A portal's rendering related things
// to access the package private field of Portal, this class is not in "render" package
@Environment(EnvType.CLIENT)
public class PortalRenderInfo {
    
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
    private PortalGroup renderingGroup;
    
    public static void init() {
        Portal.clientPortalTickSignal.connect(portal -> {
            PortalRenderInfo presentation = getOptional(portal);
            if (presentation != null) {
                presentation.tick(portal);
            }
        });
        
        Portal.portalCacheUpdateSignal.connect(portal -> {
            if (portal.level.isClientSide()) {
                PortalRenderInfo renderInfo = getOptional(portal);
                if (renderInfo != null) {
                    renderInfo.onPortalCacheUpdate(portal);
                }
            }
        });
        
        Portal.portalDisposeSignal.connect(portal -> {
            if (portal.level.isClientSide()) {
                PortalRenderInfo renderInfo = getOptional(portal);
                if (renderInfo != null) {
                    renderInfo.dispose();
                    renderInfo.setGroup(portal, null);
                }
            }
        });
    }
    
    @Nullable
    public static PortalRenderInfo getOptional(Portal portal) {
        Validate.isTrue(portal.level.isClientSide());
        
        return portal.portalRenderInfo;
    }
    
    public static PortalRenderInfo get(Portal portal) {
        Validate.isTrue(portal.level.isClientSide());
        
        if (portal.portalRenderInfo == null) {
            portal.portalRenderInfo = new PortalRenderInfo();
        }
        return portal.portalRenderInfo;
    }
    
    public PortalRenderInfo() {
    
    }
    
    private void tick(Portal portal) {
        Validate.isTrue(portal.level.isClientSide());
        
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
        disposeInfoMap((Map<List<UUID>, Visibility>) this.infoMap);
    }
    
    private static void disposeInfoMap(Map<List<UUID>, Visibility> infoMap) {
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
                disposeInfoMap(infoMap);
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
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        
        boolean decision;
        if (IPGlobal.offsetOcclusionQuery && portal instanceof Portal) {
            PortalRenderInfo renderInfo = get(((Portal) portal));
            
            List<UUID> renderingDescription = WorldRenderInfo.getRenderingDescription();
            
            Visibility visibility = renderInfo.getVisibility(renderingDescription);
            
            GlQueryObject lastFrameQuery = visibility.lastFrameQuery;
            GlQueryObject thisFrameQuery = visibility.acquireThisFrameQuery();
            
            thisFrameQuery.performQueryAnySamplePassed(queryRendering);
            
            boolean noPredict =
                renderInfo.isFrequentlyMispredicted() ||
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
                    renderInfo.updatePredictionStatus(visibility, decision);
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
    
    private void onPortalCacheUpdate(Portal portal) {
        needsGroupingUpdate = true;
        setGroup(portal, null);
    }
    
    private void setGroup(Portal portal, @Nullable PortalGroup group) {
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
        
        if (!IPGlobal.enablePortalRenderingMerge) {
            return;
        }
        
        if (!canMerge(portal)) {
            return;
        }
        
        List<Portal> nearbyPortals = McHelper.findEntitiesByBox(
            Portal.class,
            portal.getOriginWorld(),
            portal.getBoundingBox().inflate(0.5),
            Math.min(64, portal.getSizeEstimation()) * 2 + 5,
            p -> p != portal && !Portal.isFlippedPortal(p, portal) && canMerge(p)
        );
        
        Portal.TransformationDesc thisDesc = portal.getTransformationDesc();
        
        for (Portal that : nearbyPortals) {
            PortalRenderInfo nearbyPortalPresentation = get(that);
            
            PortalGroup itsGroup = nearbyPortalPresentation.renderingGroup;
            if (itsGroup != null) {
                //flipped portal pairs cannot be in the same group
                if (itsGroup.portals.stream().noneMatch(p -> Portal.isFlippedPortal(p, portal))) {
                    if (Portal.TransformationDesc.isRoughlyEqual(itsGroup.transformationDesc, thisDesc)) {
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
            }
            else {
                Portal.TransformationDesc itsDesc = that.getTransformationDesc();
                if (Portal.TransformationDesc.isRoughlyEqual(thisDesc, itsDesc)) {
                    if (renderingGroup == null) {
                        // this and that are not in any group
                        PortalGroup newGroup = new PortalGroup(thisDesc);
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
    
    private static boolean canMerge(Portal p) {
        if (IPGlobal.forceMergePortalRendering) {
            return true;
        }
        if (!p.isVisible()) {
            return false;
        }
        
        return p.isRenderingMergable();
    }
    
    @Nullable
    public static PortalGroup getGroupOf(Portal portal) {
        Validate.isTrue(!portal.getIsGlobal());
        
        PortalRenderInfo portalRenderInfo = getOptional(portal);
        
        if (portalRenderInfo == null) {
            return null;
        }
        
        return portalRenderInfo.renderingGroup;
    }
    
    private static void mergeGroup(PortalGroup g1, PortalGroup g2) {
        if (g1 == g2) {
            return;
        }
        
        ArrayList<Portal> g2Portals = new ArrayList<>(g2.portals);
        for (Portal portal : g2Portals) {
            get(portal).setGroup(portal, g1);
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        
        // normally if a portal is removed by calling remove() it will dispose normally
        // but that cannot be guaranteed
        // use this to avoid potential resource leak
        IPGlobal.preTotalRenderTaskList.addTask(() -> {
//            if (!infoMap.isEmpty()) {
//                Helper.err("A PortalRenderInfo is not being deterministically disposed");
//            }
            dispose();
            return true;
        });
    }
}
