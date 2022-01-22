package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.chunk_loading.PerformanceLevel;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;
import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.imm_ptl.core.portal.nether_portal.BlockTraverse;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.util.ArrayDeque;
import java.util.Stack;

// discover visible sections by breadth-first traverse, for portal rendering
// probably faster than vanilla
// no multi-threading because portal rendering camera views are very dynamic which is not suitable for that
public class VisibleSectionDiscovery {
    
    private static MyBuiltChunkStorage builtChunks;
    private static Frustum vanillaFrustum;
    private static ObjectArrayList<WorldRenderer.ChunkInfo> resultHolder;
    private static final ArrayDeque<ChunkBuilder.BuiltChunk> tempQueue = new ArrayDeque<>();
    private static ChunkSectionPos cameraSectionPos;
    private static long timeMark;
    private static int viewDistance;
    
    public static void discoverVisibleSections(
        ClientWorld world,
        MyBuiltChunkStorage builtChunks_,
        Camera camera,
        Frustum vanillaFrustum_,
        ObjectArrayList<WorldRenderer.ChunkInfo> resultHolder_
    ) {
        builtChunks = builtChunks_;
        vanillaFrustum = vanillaFrustum_;
        resultHolder = resultHolder_;
        
        resultHolder.clear();
        tempQueue.clear();
        
        updateViewDistance();
        
        timeMark = System.nanoTime();
        
        Vec3d cameraPos = camera.getPos();
        vanillaFrustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        cameraSectionPos = ChunkSectionPos.from(new BlockPos(cameraPos));
        
        if (cameraPos.y < world.getBottomY()) {
            discoverBottomOrTopLayerVisibleChunks(builtChunks.minSectionY);
        }
        else if (cameraPos.y > world.getTopY()) {
            discoverBottomOrTopLayerVisibleChunks(builtChunks.endSectionY - 1);
        }
        else {
            checkSection(
                cameraSectionPos.getSectionX(),
                cameraSectionPos.getSectionY(),
                cameraSectionPos.getSectionZ(),
                true
            );
        }
        
        // breadth-first searching
        while (!tempQueue.isEmpty()) {
            ChunkBuilder.BuiltChunk curr = tempQueue.poll();
            int cx = ChunkSectionPos.getSectionCoord(curr.getOrigin().getX());
            int cy = ChunkSectionPos.getSectionCoord(curr.getOrigin().getY());
            int cz = ChunkSectionPos.getSectionCoord(curr.getOrigin().getZ());
            
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
    private static boolean isVisible(ChunkBuilder.BuiltChunk builtChunk) {
        Box box = builtChunk.boundingBox;
        return vanillaFrustum.isVisible(box);
    }
    
    private static void discoverBottomOrTopLayerVisibleChunks(int cy) {
        BlockTraverse.<Object>searchOnPlane(
            cameraSectionPos.getSectionX(),
            cameraSectionPos.getSectionZ(),
            viewDistance - 1,
            (cx, cz) -> {
                checkSection(cx, cy, cz, false);
                return null;
            }
        );
    }
    
    private static void checkSection(int cx, int cy, int cz, boolean skipFrustumTest) {
        if (Math.abs(cx - cameraSectionPos.getSectionX()) > viewDistance) {
            return;
        }
        if (Math.abs(cy - cameraSectionPos.getSectionY()) > viewDistance) {
            return;
        }
        if (Math.abs(cz - cameraSectionPos.getSectionZ()) > viewDistance) {
            return;
        }
        
        ChunkBuilder.BuiltChunk builtChunk =
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
    
    private static final Stack<ObjectArrayList<WorldRenderer.ChunkInfo>> listCaches = new Stack<>();
    
    public static ObjectArrayList<WorldRenderer.ChunkInfo> takeList() {
        if (listCaches.isEmpty()) {
            return new ObjectArrayList<>();
        }
        else {
            return listCaches.pop();
        }
    }
    
    public static void returnList(ObjectArrayList<WorldRenderer.ChunkInfo> list) {
        list.clear();// avoid memory leak
        listCaches.push(list);
    }
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(() -> {
            listCaches.clear();
            resultHolder = null;
            builtChunks = null;
            vanillaFrustum = null;
        });
    }
    
}
