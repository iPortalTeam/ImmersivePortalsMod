package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;
import qouteall.imm_ptl.core.portal.nether_portal.BlockTraverse;

import java.util.ArrayDeque;

// discover visible sections by breadth-first traverse, for portal rendering
// probably faster than vanilla
// no multi-threading because portal rendering camera views are very dynamic which is not suitable for that
public class VisibleSectionDiscovery {
    
    private static MyBuiltChunkStorage builtChunks;
    private static Frustum vanillaFrustum;
    private static FrustumCuller frustumCuller;
    private static ObjectArrayList<WorldRenderer.ChunkInfo> resultHolder;
    private static final ArrayDeque<ChunkBuilder.BuiltChunk> tempQueue = new ArrayDeque<>();
    private static ChunkSectionPos cameraSectionPos;
    private static long timeMark;
    private static int viewDistance;
    
    public static void discoverVisibleSections(
        ClientWorld world,
        WorldRenderer worldRenderer_,
        MyBuiltChunkStorage builtChunks_,
        Camera camera,
        Frustum vanillaFrustum_,
        FrustumCuller frustumCuller_,
        ObjectArrayList<WorldRenderer.ChunkInfo> resultHolder_
    ) {
        builtChunks = builtChunks_;
        vanillaFrustum = vanillaFrustum_;
        resultHolder = resultHolder_;
        
        resultHolder.clear();
        tempQueue.clear();
        
        viewDistance = MinecraftClient.getInstance().options.viewDistance;
        
        timeMark = System.nanoTime();
        
        Vec3d cameraPos = camera.getPos();
        cameraSectionPos = ChunkSectionPos.from(new BlockPos(cameraPos));
        
        if (cameraPos.y < world.getBottomY()) {
            discoverBottomOrTopLayerVisibleChunks(builtChunks.minSectionY);
        }
        else if (cameraPos.y > world.getTopY()) {
            discoverBottomOrTopLayerVisibleChunks(builtChunks.endSectionY - 1);
        }
        else {
            ChunkBuilder.BuiltChunk startPoint = builtChunks.rawGet(
                cameraSectionPos.getSectionX(),
                cameraSectionPos.getSectionY(),
                cameraSectionPos.getSectionZ(),
                timeMark
            );
            if (startPoint == null) {
                return;
            }
            tempQueue.add(startPoint);
        }
        
        // breadth-first searching
        while (!tempQueue.isEmpty()) {
            ChunkBuilder.BuiltChunk curr = tempQueue.poll();
            int cx = ChunkSectionPos.getSectionCoord(curr.getOrigin().getX());
            int cy = ChunkSectionPos.getSectionCoord(curr.getOrigin().getY());
            int cz = ChunkSectionPos.getSectionCoord(curr.getOrigin().getZ());
            
            checkSection(cx + 1, cy, cz);
            checkSection(cx - 1, cy, cz);
            checkSection(cx, cy + 1, cz);
            checkSection(cx, cy - 1, cz);
            checkSection(cx, cy, cz + 1);
            checkSection(cx, cy, cz - 1);
        }
        
        // avoid memory leak
        resultHolder = null;
        builtChunks = null;
        vanillaFrustum = null;
        frustumCuller = null;
    }
    
    private static boolean isVisible(ChunkBuilder.BuiltChunk builtChunk) {
        Box box = builtChunk.boundingBox;
        if (!vanillaFrustum.isVisible(box)) {
            return false;
        }
        return !frustumCuller.canDetermineInvisible(
            box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ
        );
    }
    
    private static void discoverBottomOrTopLayerVisibleChunks(int cy) {
        BlockTraverse.<Object>searchOnPlane(
            cameraSectionPos.getSectionX(),
            cameraSectionPos.getMaxZ(),
            viewDistance - 1,
            (cx, cz) -> {
                checkSection(cy, cx, cz);
                return null;
            }
        );
    }
    
    private static void checkSection(int cy, int cx, int cz) {
        ChunkBuilder.BuiltChunk builtChunk =
            builtChunks.rawGet(cx, cy, cz, timeMark);
        if (builtChunk != null) {
            IEBuiltChunk ieBuiltChunk = (IEBuiltChunk) builtChunk;
            if (ieBuiltChunk.portal_getMark() != timeMark) {
                ieBuiltChunk.portal_setMark(timeMark);// mark it checked
                if (isVisible(builtChunk)) {
                    tempQueue.add(builtChunk);
                    resultHolder.add(ieBuiltChunk.portal_getDummyChunkInfo());
                }
            }
        }
    }
    
}
