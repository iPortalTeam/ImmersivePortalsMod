package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MyViewFrustum /*extends ChunkRenderDispatcher*/ {
//    public static final int maxIdleChunkNum = 500;
//
//    public static boolean enableFrustumSubstitution = true;
//
//    protected ChunkRendererFactory ChunkRendererFactory;
//    protected Map<BlockPos, ChunkRenderer> ChunkRendererMap = new HashMap<>();
//    protected Map<ChunkRenderer, Long> lastActiveNanoTime = new HashMap<>();
//    protected Deque<ChunkRenderer> idleChunks = new ArrayDeque<>();
//    protected int renderDistance;
//
//    protected MyViewFrustum(
//        World world,
//        int renderDistanceChunks,
//        WorldRenderer worldRenderer,
//        ChunkRendererFactory chunkRendererFactory,
//        ChunkRenderDispatcher oldViewFrustum
//    ) {
//        super(world, renderDistanceChunks, worldRenderer, chunkRendererFactory);
//
//        this.renderDistance = renderDistanceChunks;
//        this.ChunkRendererFactory = chunkRendererFactory;
//        this.renderers = new ChunkRenderer[this.sizeX * this.sizeY * this.sizeZ];
//
//        takeOverChunkRenderersFrom(oldViewFrustum);
//
//        ModMain.clientTickSignal.connectWithWeakRef(this, (this_) -> {
//            ClientWorld worldClient = MinecraftClient.getInstance().world;
//            if (worldClient != null) {
//                if (worldClient.getTime() % 147 == 0) {
//                    this_.dismissInactiveChunkRenderers();
//                }
//            }
//        });
//    }
//
//    public static void tryToSubstitute(
//        World world,
//        WorldRenderer worldRenderer
//    ) {
//        if (!enableFrustumSubstitution) {
//            return;
//        }
//
//        if (!(worldRenderer.chunkRenderDispatcher instanceof MyViewFrustum)) {
//            worldRenderer.chunkRenderDispatcher = new MyViewFrustum(
//                world,
//                MinecraftClient.getInstance().options.viewDistance,
//                worldRenderer,
//                ChunkRenderer::new,
//                worldRenderer.chunkRenderDispatcher
//            );
//            Helper.log("Successfully substituted ViewFrustum");
//        }
//    }
//
//    public static void updateViewFrustum() {
//        MinecraftClient mc = MinecraftClient.getInstance();
//
//        assert mc.player.world == mc.world;
//
//        WorldRenderer worldRenderer = Globals.portalManagerClient.worldLoader.getWorldRenderer(
//            mc.world.dimension.getType()
//        );
//        tryToSubstitute(
//            mc.world,
//            worldRenderer
//        );
//
//        worldRenderer.chunkRenderDispatcher.updateChunkPositions(
//            mc.player.x,
//            mc.player.z
//        );
//    }
//
//    public static void resetModelViewMatricesOfChunkRenderers(WorldRenderer renderGlobal) {
//        Globals.portalRenderManager.taskList.addTask(() -> {
//            if(!(renderGlobal.chunkRenderDispatcher instanceof MyViewFrustum)){
//                Helper.err("Teleport upon entering game?");
//            }
//
//            MyViewFrustum.updateViewFrustum();
//
//            MyViewFrustum myViewFrustum = (MyViewFrustum) renderGlobal.chunkRenderDispatcher;
//
//            myViewFrustum.getChunkRenderers().forEach(
//                ChunkRenderer -> {
//                    if (ChunkRenderer != null) {
//                        ChunkRenderer.initModelviewMatrix();
//                    }
//                }
//            );
//
//            return true;
//        });
//    }
//
//    private void takeOverChunkRenderersFrom(ChunkRenderDispatcher oldViewFrustum) {
//        Arrays.stream(oldViewFrustum.renderers).forEach(
//            ChunkRenderer -> {
//                ChunkRendererMap.put(ChunkRenderer.getPosition(), ChunkRenderer);
//                updateLastUsedTime(ChunkRenderer);
//            }
//        );
//        oldViewFrustum.renderers = new ChunkRenderer[0];
//        oldViewFrustum.deleteGlResources();
//    }
//
//    private ChunkRenderer findAndEmployChunkRenderer(BlockPos basePos) {
//        assert !ChunkRendererMap.containsKey(basePos);
//
//        ChunkRenderer ChunkRenderer = idleChunks.pollLast();
//
//        if (ChunkRenderer == null) {
//            ChunkRenderer = ChunkRendererFactory.create(world, renderer);
//        }
//
//        employChunkRenderer(ChunkRenderer, basePos);
//
//        return ChunkRenderer;
//    }
//
//    private void employChunkRenderer(ChunkRenderer ChunkRenderer, BlockPos basePos) {
//        ChunkRenderer.setPosition(basePos.getX(), basePos.getY(), basePos.getZ());
//        ChunkRendererMap.put(ChunkRenderer.getPosition(), ChunkRenderer);
//        updateLastUsedTime(ChunkRenderer);
//    }
//
//    private void dismissChunkRenderer(BlockPos basePos) {
//        assert ChunkRendererMap.containsKey(basePos);
//
//        ChunkRenderer ChunkRenderer = ChunkRendererMap.remove(basePos);
//
//        assert lastActiveNanoTime.containsKey(ChunkRenderer);
//
//        lastActiveNanoTime.remove(ChunkRenderer);
//
//        idleChunks.addLast(ChunkRenderer);
//
//        destructAbundantIdleChunks();
//    }
//
//    private void destructAbundantIdleChunks() {
//        if (idleChunks.size() > maxIdleChunkNum) {
//            int toDestructChunkRenderersNum = idleChunks.size() - maxIdleChunkNum;
//            IntStream.range(0, toDestructChunkRenderersNum).forEach(n -> {
//                ChunkRenderer ChunkRendererToDestruct = idleChunks.pollFirst();
//                assert ChunkRendererToDestruct != null;
//                ChunkRendererToDestruct.deleteGlResources();
//            });
//        }
//    }
//
//    private void dismissInactiveChunkRenderers() {
//        long currentTime = System.nanoTime();
//        final long deletingValve = 1000000000L * 30;//30 seconds
//        //NOTE if you miss 'L' then it will overflow
//
//        for (ChunkRenderer ChunkRenderer : this.renderers) {
//            //make sure none of render chunk get dismissed
//            updateLastUsedTime(ChunkRenderer);
//        }
//
//        ArrayDeque<ChunkRenderer> ChunkRenderersToDismiss = lastActiveNanoTime.entrySet().stream()
//            .filter(
//                entry -> currentTime - entry.getValue() > deletingValve
//            )
//            .map(
//                Map.Entry::getKey
//            )
//            .collect(Collectors.toCollection(ArrayDeque::new));
//
//        if (!ChunkRenderersToDismiss.isEmpty()) {
//            Helper.log(String.format(
//                "dismissed %d render chunks",
//                ChunkRenderersToDismiss.size()
//            ));
//        }
//
//        ChunkRenderersToDismiss.forEach(
//            ChunkRenderer -> dismissChunkRenderer(ChunkRenderer.getPosition())
//        );
//    }
//
//    @Override
//    protected void createChunkRenderers(IChunkRendererFactory ChunkRendererFactory_) {
//        //do nothing
//    }
//
//    public Collection<ChunkRenderer> getChunkRenderers() {
//        return ChunkRendererMap.values();
//    }
//
//    @Override
//    public void updateChunkPositions(double viewEntityX, double viewEntityZ) {
//
//        int px = MathHelper.floor(viewEntityX) - 8;
//        int pz = MathHelper.floor(viewEntityZ) - 8;
//        int maxLen = this.sizeX * 16;
//
//        for (int cx = 0; cx < this.sizeX; ++cx) {
//            int posX = this.getBaseCoordinate(px, maxLen, cx);
//
//            for (int cz = 0; cz < this.sizeZ; ++cz) {
//                int posZ = this.getBaseCoordinate(pz, maxLen, cz);
//
//                for (int cy = 0; cy < this.sizeY; ++cy) {
//                    int posY = cy * 16;
//
//                    ((ChunkRenderDispatcher) this).renderers[this.func_212478_a(cx, cy, cz)] =
//                        myGetChunkRenderer(
//                            new BlockPos(posX, posY, posZ)
//                        );
//                }
//            }
//        }
//    }
//
//    @Override
//    public void deleteGlResources() {
//        ChunkRendererMap.values().forEach(ChunkRenderer -> ChunkRenderer.deleteGlResources());
//        idleChunks.forEach(ChunkRenderer -> ChunkRenderer.deleteGlResources());
//
//        ChunkRendererMap.clear();
//        lastActiveNanoTime.clear();
//        idleChunks.clear();
//    }
//
//    @Override
//    public void markBlocksForUpdate(
//        int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean updateImmediately
//    ) {
//        int cMinX = MathHelper.intFloorDiv(minX, 16);
//        int cMinY = MathHelper.intFloorDiv(minY, 16);
//        int cMinZ = MathHelper.intFloorDiv(minZ, 16);
//        int cMaxX = MathHelper.intFloorDiv(maxX, 16);
//        int cMaxY = MathHelper.intFloorDiv(maxY, 16);
//        int cMaxZ = MathHelper.intFloorDiv(maxZ, 16);
//
//        for (int cx = cMinX; cx <= cMaxX; ++cx) {
//            int l1 = MathHelper.normalizeAngle(cx, this.sizeX);
//
//            for (int cy = cMinY; cy <= cMaxY; ++cy) {
//                int j2 = MathHelper.normalizeAngle(cy, this.sizeY);
//
//                for (int cz = cMinZ; cz <= cMaxZ; ++cz) {
//                    myGetChunkRenderer(new BlockPos(cx * 16, cy * 16, cz * 16))
//                        .setNeedsUpdate(updateImmediately);
//                }
//            }
//        }
//    }
//
//    public void forceDismissChunkRenderer(
//        BlockPos pos
//    ) {
//        BlockPos basePos = getBasePos(pos);
//        ChunkRenderer ChunkRenderer = ChunkRendererMap.get(basePos);
//        if (ChunkRenderer == null) return;
//        for (int i = 0; i < renderers.length; i++) {
//            if (renderers[i] == ChunkRenderer) {
//                renderers[i] = null;
//            }
//        }
//        dismissChunkRenderer(basePos);
//    }
//
//    @Nullable
//    @Override
//    //NOTE input the block coordinate, or strange coordinate
//    public ChunkRenderer getChunkRenderer(BlockPos blockPos) {
//        return super.getChunkRenderer(blockPos);
//    }
//
//    private ChunkRenderer myGetChunkRenderer(BlockPos blockPos) {
//        BlockPos basePos = getBasePos(blockPos);
//
//        if (!ChunkRendererMap.containsKey(basePos)) {
//            return findAndEmployChunkRenderer(basePos);
//        }
//        else {
//            ChunkRenderer ChunkRenderer = ChunkRendererMap.get(basePos);
//            updateLastUsedTime(ChunkRenderer);
//            return ChunkRenderer;
//        }
//    }
//
//    private static BlockPos getBasePos(BlockPos blockPos) {
//        return new BlockPos(
//            MathHelper.intFloorDiv(blockPos.getX(), 16) * 16,
//            MathHelper.intFloorDiv(blockPos.getY(), 16) * 16,
//            MathHelper.intFloorDiv(blockPos.getZ(), 16) * 16
//        );
//    }
//
//    private void updateLastUsedTime(ChunkRenderer ChunkRenderer) {
//        if (ChunkRenderer == null) {
//            return;
//        }
//        lastActiveNanoTime.put(ChunkRenderer, System.nanoTime());
//    }
}
