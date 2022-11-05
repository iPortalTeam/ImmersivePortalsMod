package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.chunk_loading.PerformanceLevel;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;
import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.imm_ptl.core.portal.nether_portal.BlockTraverse;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.util.ArrayDeque;
import java.util.Stack;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Discover visible sections by breadth-first traverse, for portal rendering.
 * Probably faster than vanilla (because no cave culling and garbage object allocation).
 * No multi-threading because portal rendering camera views are very dynamic which is not suitable for that.
 * No cave culling because vanilla has a multithreaded cave culling that's very
 *  hard to integrate with portal rendering.
 * The cave culling is conditionally enabled with Sodium: {@link PortalRendering#shouldEnableSodiumCaveCulling()}
 */
@Environment(EnvType.CLIENT)
public class VisibleSectionDiscovery {
    
    private static MyBuiltChunkStorage builtChunks;
    private static Frustum vanillaFrustum;
    private static ObjectArrayList<LevelRenderer.RenderChunkInfo> resultHolder;
    private static final ArrayDeque<ChunkRenderDispatcher.RenderChunk> tempQueue = new ArrayDeque<>();
    private static SectionPos cameraSectionPos;
    private static long timeMark;
    private static int viewDistance;
    
    public static void discoverVisibleSections(
        ClientLevel world,
        MyBuiltChunkStorage builtChunks_,
        Camera camera,
        Frustum vanillaFrustum_,
        ObjectArrayList<LevelRenderer.RenderChunkInfo> resultHolder_
    ) {
        builtChunks = builtChunks_;
        vanillaFrustum = vanillaFrustum_;
        resultHolder = resultHolder_;
        
        resultHolder.clear();
        tempQueue.clear();
        
        updateViewDistance();
        
        timeMark = System.nanoTime();
        
        Vec3 cameraPos = camera.getPosition();
        vanillaFrustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
        cameraSectionPos = SectionPos.of(new BlockPos(cameraPos));
        
        if (cameraPos.y < world.getMinBuildHeight()) {
            discoverBottomOrTopLayerVisibleChunks(builtChunks.minSectionY);
        }
        else if (cameraPos.y > world.getMaxBuildHeight()) {
            discoverBottomOrTopLayerVisibleChunks(builtChunks.endSectionY - 1);
        }
        else {
            checkSection(
                cameraSectionPos.x(),
                cameraSectionPos.y(),
                cameraSectionPos.z(),
                true
            );
        }
        
        // breadth-first searching
        while (!tempQueue.isEmpty()) {
            ChunkRenderDispatcher.RenderChunk curr = tempQueue.poll();
            int cx = SectionPos.blockToSectionCoord(curr.getOrigin().getX());
            int cy = SectionPos.blockToSectionCoord(curr.getOrigin().getY());
            int cz = SectionPos.blockToSectionCoord(curr.getOrigin().getZ());
            
            checkSection(cx + 1, cy, cz, false);
            checkSection(cx - 1, cy, cz, false);
            checkSection(cx, cy + 1, cz, false);
            checkSection(cx, cy - 1, cz, false);
            checkSection(cx, cy, cz + 1, false);
            checkSection(cx, cy, cz - 1, false);
        }
        
        // avoid memory leak
        resultHolder = null;
        builtChunks = null;
        vanillaFrustum = null;
    }
    
    private static void updateViewDistance() {
        int distance = WorldRenderInfo.getRenderDistance();
        viewDistance = PerformanceLevel.getPortalRenderingDistance(
            ClientPerformanceMonitor.level, distance
        );
    }
    
    // NOTE the vanilla frustum culling code may wrongly cull the first section
    private static boolean isVisible(ChunkRenderDispatcher.RenderChunk builtChunk) {
        AABB box = builtChunk.getBoundingBox();
        return vanillaFrustum.isVisible(box);
    }
    
    private static void discoverBottomOrTopLayerVisibleChunks(int cy) {
        BlockTraverse.<Object>searchOnPlane(
            cameraSectionPos.x(),
            cameraSectionPos.z(),
            viewDistance - 1,
            (cx, cz) -> {
                checkSection(cx, cy, cz, false);
                return null;
            }
        );
    }
    
    private static void checkSection(int cx, int cy, int cz, boolean skipFrustumTest) {
        if (Math.abs(cx - cameraSectionPos.x()) > viewDistance) {
            return;
        }
        if (Math.abs(cy - cameraSectionPos.y()) > viewDistance) {
            return;
        }
        if (Math.abs(cz - cameraSectionPos.z()) > viewDistance) {
            return;
        }
        
        ChunkRenderDispatcher.RenderChunk builtChunk =
            builtChunks.rawFetch(cx, cy, cz, timeMark);
        if (builtChunk != null) {
            IEBuiltChunk ieBuiltChunk = (IEBuiltChunk) builtChunk;
            if (ieBuiltChunk.portal_getMark() != timeMark) {
                ieBuiltChunk.portal_setMark(timeMark);// mark it checked
                if (skipFrustumTest || isVisible(builtChunk)) {
                    tempQueue.add(builtChunk);
                    resultHolder.add(ieBuiltChunk.portal_getDummyChunkInfo());
                }
            }
        }
    }
    
    private static final Stack<ObjectArrayList<LevelRenderer.RenderChunkInfo>> listCaches = new Stack<>();
    
    public static ObjectArrayList<LevelRenderer.RenderChunkInfo> takeList() {
        if (listCaches.isEmpty()) {
            return new ObjectArrayList<>();
        }
        else {
            return listCaches.pop();
        }
    }
    
    public static void returnList(ObjectArrayList<LevelRenderer.RenderChunkInfo> list) {
        list.clear();// avoid memory leak
        listCaches.push(list);
    }
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(VisibleSectionDiscovery::cleanUp);
        
        ClientWorldLoader.clientDimensionDynamicRemoveSignal.connect((dim) -> {
            cleanUp();
        });
    }
    
    private static void cleanUp() {
        listCaches.clear();
        resultHolder = null;
        builtChunks = null;
        vanillaFrustum = null;
    }
    
}
