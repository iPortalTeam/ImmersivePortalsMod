package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.sun.istack.internal.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MyViewFrustum extends ChunkRenderDispatcher {
    public static final int maxIdleChunkNum = 500;
    
    public static boolean enableFrustumSubstitution = true;
    
    protected ChunkRendererFactory chunkRendererFactory;
    protected Map<BlockPos, ChunkRenderer> chunkRendererMap = new HashMap<>();
    protected Map<ChunkRenderer, Long> lastActiveNanoTime = new HashMap<>();
    protected Deque<ChunkRenderer> idleChunks = new ArrayDeque<>();
    protected int renderDistance;
    
    public MyViewFrustum(
        World world,
        int renderDistanceChunks,
        WorldRenderer worldRenderer,
        ChunkRendererFactory chunkRendererFactory,
        ChunkRenderDispatcher oldViewFrustum
    ) {
        super(world, renderDistanceChunks, worldRenderer, chunkRendererFactory);
        
        this.renderDistance = renderDistanceChunks;
        this.chunkRendererFactory = chunkRendererFactory;
        this.renderers = new ChunkRenderer[this.sizeX * this.sizeY * this.sizeZ];
        
        ModMain.clientTickSignal.connectWithWeakRef(this, (this_) -> {
            ClientWorld worldClient = MinecraftClient.getInstance().world;
            if (worldClient != null) {
                if (worldClient.getTime() % 147 == 0) {
                    this_.dismissInactiveChunkRenderers();
                }
            }
        });
        
        takeOverRenderChunksFrom(oldViewFrustum);
    }
    
    private void takeOverRenderChunksFrom(ChunkRenderDispatcher oldViewFrustum) {
        Arrays.stream(oldViewFrustum.renderers).forEach(
            renderChunk -> {
                chunkRendererMap.put(renderChunk.getOrigin(), renderChunk);
                updateLastUsedTime(renderChunk);
            }
        );
        this.renderers = oldViewFrustum.renderers;
        
        oldViewFrustum.renderers = new ChunkRenderer[0];
        oldViewFrustum.delete();
    }
    
    private ChunkRenderer findAndEmployChunkRenderer(BlockPos basePos) {
        assert !chunkRendererMap.containsKey(basePos);
        
        ChunkRenderer ChunkRenderer = idleChunks.pollLast();
        
        if (ChunkRenderer == null) {
            ChunkRenderer = chunkRendererFactory.create(world, renderer);
        }
        
        employChunkRenderer(ChunkRenderer, basePos);
        
        return ChunkRenderer;
    }
    
    private void employChunkRenderer(ChunkRenderer chunkRenderer, BlockPos basePos) {
        chunkRenderer.setOrigin(basePos.getX(), basePos.getY(), basePos.getZ());
        chunkRendererMap.put(chunkRenderer.getOrigin(), chunkRenderer);
        updateLastUsedTime(chunkRenderer);
    }
    
    private void dismissChunkRenderer(BlockPos basePos) {
        assert chunkRendererMap.containsKey(basePos);
        
        ChunkRenderer ChunkRenderer = chunkRendererMap.remove(basePos);
        
        assert lastActiveNanoTime.containsKey(ChunkRenderer);
        
        lastActiveNanoTime.remove(ChunkRenderer);
        
        idleChunks.addLast(ChunkRenderer);
        
        destructAbundantIdleChunks();
    }
    
    private void destructAbundantIdleChunks() {
        if (idleChunks.size() > maxIdleChunkNum) {
            int toDestructChunkRenderersNum = idleChunks.size() - maxIdleChunkNum;
            IntStream.range(0, toDestructChunkRenderersNum).forEach(n -> {
                ChunkRenderer chunkRendererToDestruct = idleChunks.pollFirst();
                assert chunkRendererToDestruct != null;
                chunkRendererToDestruct.delete();
            });
        }
    }
    
    private void dismissInactiveChunkRenderers() {
        long currentTime = System.nanoTime();
        final long deletingValve = 1000000000L * 30;//30 seconds
        //NOTE if you miss 'L' then it will overflow
        
        for (ChunkRenderer ChunkRenderer : this.renderers) {
            //make sure none of render chunk get dismissed
            updateLastUsedTime(ChunkRenderer);
        }
        
        ArrayDeque<ChunkRenderer> ChunkRenderersToDismiss = lastActiveNanoTime.entrySet().stream()
            .filter(
                entry -> currentTime - entry.getValue() > deletingValve
            )
            .map(
                Map.Entry::getKey
            )
            .collect(Collectors.toCollection(ArrayDeque::new));
        
        if (!ChunkRenderersToDismiss.isEmpty()) {
            Helper.log(String.format(
                "dismissed %d render chunks",
                ChunkRenderersToDismiss.size()
            ));
        }
        
        ChunkRenderersToDismiss.forEach(
            ChunkRenderer -> dismissChunkRenderer(ChunkRenderer.getOrigin())
        );
    }
    
    public Collection<ChunkRenderer> getChunkRenderers() {
        return chunkRendererMap.values();
    }
    
    @Override
    public void updateCameraPosition(double viewEntityX, double viewEntityZ) {
        
        int px = MathHelper.floor(viewEntityX) - 8;
        int pz = MathHelper.floor(viewEntityZ) - 8;
        
        int maxLen = this.sizeX * 16;
        
        for (int cx = 0; cx < this.sizeX; ++cx) {
            int posX = this.method_3328(px, maxLen, cx);
            
            for (int cz = 0; cz < this.sizeZ; ++cz) {
                int posZ = this.method_3328(pz, maxLen, cz);
                
                for (int cy = 0; cy < this.sizeY; ++cy) {
                    int posY = cy * 16;
                    
                    renderers[this.getChunkIndex(cx, cy, cz)] =
                        myGetChunkRenderer(
                            new BlockPos(posX, posY, posZ)
                        );
                }
            }
        }
    }
    
    @Override
    public void delete() {
        chunkRendererMap.values().forEach(ChunkRenderer::delete);
        idleChunks.forEach(ChunkRenderer::delete);
        
        chunkRendererMap.clear();
        lastActiveNanoTime.clear();
        idleChunks.clear();
    }
    
    public void forceDismissChunkRenderer(
        BlockPos pos
    ) {
        BlockPos basePos = getBasePos(pos);
        ChunkRenderer ChunkRenderer = chunkRendererMap.get(basePos);
        if (ChunkRenderer == null) return;
        for (int i = 0; i < renderers.length; i++) {
            if (renderers[i] == ChunkRenderer) {
                renderers[i] = null;
            }
        }
        dismissChunkRenderer(basePos);
    }
    
    //NOTE input block pos instead of chunk pos
    private ChunkRenderer myGetChunkRenderer(BlockPos blockPos) {
        BlockPos basePos = getBasePos(blockPos);
        
        if (!chunkRendererMap.containsKey(basePos)) {
            return findAndEmployChunkRenderer(basePos);
        }
        else {
            ChunkRenderer chunkRenderer = chunkRendererMap.get(basePos);
            updateLastUsedTime(chunkRenderer);
            return chunkRenderer;
        }
    }
    
    private static BlockPos getBasePos(BlockPos blockPos) {
        return new BlockPos(
            MathHelper.floorDiv(blockPos.getX(), 16) * 16,
            MathHelper.floorDiv(blockPos.getY(), 16) * 16,
            MathHelper.floorDiv(blockPos.getZ(), 16) * 16
        );
    }
    
    private void updateLastUsedTime(ChunkRenderer ChunkRenderer) {
        if (ChunkRenderer == null) {
            return;
        }
        lastActiveNanoTime.put(ChunkRenderer, System.nanoTime());
    }
    
    @Override
    public void scheduleChunkRender(int chunkX, int chunkY, int chunkZ, boolean boolean_1) {
        ChunkRenderer chunkRenderer = myGetChunkRenderer(new BlockPos(
            chunkX * 16, chunkY * 16, chunkZ * 16
        ));
        
        chunkRenderer.scheduleRebuild(boolean_1);
    }
    
    //copied
    private int method_3328(int int_1, int int_2, int int_3) {
        int int_4 = int_3 * 16;
        int int_5 = int_4 - int_1 + int_2 / 2;
        if (int_5 < 0) {
            int_5 -= int_2 - 1;
        }
        
        return int_4 - int_5 / int_2 * int_2;
    }
    
    //copied
    private int getChunkIndex(int int_1, int int_2, int int_3) {
        return (int_3 * this.sizeY + int_2) * this.sizeX + int_1;
    }
}
