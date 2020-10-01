package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class PortalPresentation {
    
    // not stored inside Portal because this has its own dispose strategy
    private static final HashMap<Portal, PortalPresentation> dataMap = new HashMap<>();
    
    private static long lastPurgeTime = 0;
    
    // dispose by the last active time
    // using finalize() depends on GC and is not reliable
    private long lastActiveNanoTime;
    
    @Nullable
    public Map<List<UUID>, GlQueryObject> lastFrameQuery;
    
    @Nullable
    public Map<List<UUID>, GlQueryObject> thisFrameQuery;
    
    public int thisFrameQueryFrameIndex = -1;
    
    @Nullable
    public Boolean lastFrameRendered;
    
    @Nullable
    public Boolean thisFrameRendered;
    
    private long mispredictTime1 = 0;
    private long mispredictTime2 = 0;
    
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
    
    public PortalPresentation() {
        lastActiveNanoTime = System.nanoTime();
    }
    
    public void onUsed() {
        lastActiveNanoTime = System.nanoTime();
    }
    
    private boolean shouldDispose(long currTime) {
        return currTime - lastActiveNanoTime > Helper.secondToNano(60);
    }
    
    public void dispose() {
        disposeLastFrameQuery();
        
        if (thisFrameQuery != null) {
            for (GlQueryObject queryObject : thisFrameQuery.values()) {
                GlQueryObject.returnQueryObject(queryObject);
            }
            thisFrameQuery.clear();
        }
    }
    
    private void updateQuerySet() {
        onUsed();
        if (RenderStates.frameIndex != thisFrameQueryFrameIndex) {
            
            if (RenderStates.frameIndex == thisFrameQueryFrameIndex + 1) {
                
                disposeLastFrameQuery();
                
                lastFrameQuery = thisFrameQuery;
                thisFrameQuery = null;
                
                lastFrameRendered = thisFrameRendered;
                thisFrameRendered = null;
            }
            else {
                disposeLastFrameQuery();
                
                lastFrameRendered = null;
                thisFrameRendered = null;
            }
            
            thisFrameQueryFrameIndex = RenderStates.frameIndex;
        }
    }
    
    private void disposeLastFrameQuery() {
        if (lastFrameQuery != null) {
            for (GlQueryObject queryObject : lastFrameQuery.values()) {
                GlQueryObject.returnQueryObject(queryObject);
            }
            lastFrameQuery.clear();
        }
    }
    
    @Nullable
    public GlQueryObject getLastFrameQuery(List<UUID> desc) {
        updateQuerySet();
        if (lastFrameQuery == null) {
            return null;
        }
        return lastFrameQuery.get(desc);
    }
    
    public GlQueryObject acquireThisFrameQuery(List<UUID> desc) {
        updateQuerySet();
        if (thisFrameQuery == null) {
            thisFrameQuery = new HashMap<>();
        }
        return thisFrameQuery.computeIfAbsent(desc, k -> GlQueryObject.acquireQueryObject());
    }
    
    public void onMispredict() {
        mispredictTime1 = mispredictTime2;
        mispredictTime2 = System.nanoTime();
    }
    
    public boolean isFrequentlyMispredicted() {
        long currTime = System.nanoTime();
        
        return (currTime - mispredictTime1) < Helper.secondToNano(30);
    }
    
    public void updatePredictionStatus(boolean anySamplePassed) {
        if (anySamplePassed) {
            thisFrameRendered = true;
        }
        else {
            if (thisFrameRendered == null) {
                thisFrameRendered = false;
            }
        }
        
        if (anySamplePassed) {
            if (lastFrameRendered != null) {
                if (!lastFrameRendered) {
                    onMispredict();
                }
            }
        }
    }
    
    public static boolean renderAndQuery(Portal portal, Runnable queryRendering) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        
        boolean anySamplePassed;
        if (Global.offsetOcclusionQuery) {
            PortalPresentation presentation = get(portal);
            
            List<UUID> renderingDescription = RenderInfo.getRenderingDescription();
            GlQueryObject lastFrameQuery = presentation.getLastFrameQuery(renderingDescription);
            GlQueryObject thisFrameQuery = presentation.acquireThisFrameQuery(renderingDescription);
            
            thisFrameQuery.performQueryAnySamplePassed(queryRendering);
            
            boolean noPredict =
                presentation.isFrequentlyMispredicted() ||
                    RenderStates.getRenderedPortalNum() < 2;
            if (lastFrameQuery != null && !noPredict) {
                profiler.push("fetch_last_frame");
                anySamplePassed = lastFrameQuery.fetchQueryResult();
                profiler.pop();
            }
            else {
                profiler.push("fetch_this_frame");
                anySamplePassed = thisFrameQuery.fetchQueryResult();
                profiler.pop();
                QueryManager.queryStallCounter++;
            }
            
            presentation.updatePredictionStatus(anySamplePassed);
        }
        else {
            anySamplePassed = QueryManager.renderAndGetDoesAnySamplePass(queryRendering);
        }
        return anySamplePassed;
    }
}
