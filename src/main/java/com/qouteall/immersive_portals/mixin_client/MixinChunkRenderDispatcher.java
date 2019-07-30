package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.exposer.IEChunkRenderDispatcher;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher implements IEChunkRenderDispatcher {
    @Shadow
    @Final
    protected WorldRenderer renderer;
    @Shadow
    @Final
    protected World world;
    @Shadow
    protected int sizeY;
    @Shadow
    protected int sizeX;
    @Shadow
    protected int sizeZ;
    @Shadow
    public ChunkRenderer[] renderers;
    
    private ChunkRendererFactory factory;
    private Map<BlockPos, ChunkRenderer> chunkRendererMap;
    private Map<ChunkRenderer, Long> lastActiveNanoTime;
    private Deque<ChunkRenderer> idleChunks;
    
    @Inject(
        method = "Lnet/minecraft/client/render/ChunkRenderDispatcher;<init>(Lnet/minecraft/world/World;ILnet/minecraft/client/render/WorldRenderer;Lnet/minecraft/client/render/chunk/ChunkRendererFactory;)V",
        at = @At("RETURN")
    )
    private void onConstruct(
        World world_1,
        int renderDistanceChunks,
        WorldRenderer worldRenderer_1,
        ChunkRendererFactory chunkRendererFactory,
        CallbackInfo ci
    ) {
        this.factory = chunkRendererFactory;
        
        chunkRendererMap = new HashMap<>();
        lastActiveNanoTime = new HashMap<>();
        idleChunks = new ArrayDeque<>();
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            ((IEChunkRenderDispatcher) this),
            IEChunkRenderDispatcher::tick
        );
    
        if (Globals.useHackedChunkRenderDispatcher) {
            //it will run createChunks() before this
            for (ChunkRenderer renderChunk : renderers) {
                chunkRendererMap.put(renderChunk.getOrigin(), renderChunk);
                updateLastUsedTime(renderChunk);
            }
        }
    }
    
    @Inject(
        method = "delete",
        at = @At("HEAD"),
        cancellable = true
    )
    private void delete(CallbackInfo ci) {
        if (Globals.useHackedChunkRenderDispatcher) {
            chunkRendererMap.values().forEach(ChunkRenderer::delete);
            idleChunks.forEach(ChunkRenderer::delete);
            
            chunkRendererMap.clear();
            lastActiveNanoTime.clear();
            idleChunks.clear();
            
            ci.cancel();
        }
    }
    
    @Override
    public void tick() {
        if (Globals.useHackedChunkRenderDispatcher) {
            ClientWorld worldClient = MinecraftClient.getInstance().world;
            if (worldClient != null) {
                if (worldClient.getTime() % 147 == 0) {
                    dismissInactiveChunkRenderers();
                }
            }
        }
    }
    
    private ChunkRenderer findAndEmployChunkRenderer(BlockPos basePos) {
        assert !chunkRendererMap.containsKey(basePos);
        assert Globals.useHackedChunkRenderDispatcher;
        
        ChunkRenderer chunkRenderer = idleChunks.pollLast();
    
        if (chunkRenderer == null) {
            chunkRenderer = factory.create(world, renderer);
        }
    
        employChunkRenderer(chunkRenderer, basePos);
    
        return chunkRenderer;
    }
    
    private void employChunkRenderer(ChunkRenderer chunkRenderer, BlockPos basePos) {
        assert Globals.useHackedChunkRenderDispatcher;
        
        chunkRenderer.setOrigin(basePos.getX(), basePos.getY(), basePos.getZ());
        chunkRendererMap.put(chunkRenderer.getOrigin(), chunkRenderer);
        updateLastUsedTime(chunkRenderer);
    }
    
    private void dismissChunkRenderer(BlockPos basePos) {
        assert Globals.useHackedChunkRenderDispatcher;
        assert chunkRendererMap.containsKey(basePos);
    
        ChunkRenderer chunkRenderer = chunkRendererMap.remove(basePos);
    
        assert lastActiveNanoTime.containsKey(chunkRenderer);
    
        lastActiveNanoTime.remove(chunkRenderer);
    
        idleChunks.addLast(chunkRenderer);
        
        destructAbundantIdleChunks();
    }
    
    private void destructAbundantIdleChunks() {
        assert Globals.useHackedChunkRenderDispatcher;
        if (idleChunks.size() > Globals.maxIdleChunkRendererNum) {
            int toDestructChunkRenderersNum = idleChunks.size() - Globals.maxIdleChunkRendererNum;
            IntStream.range(0, toDestructChunkRenderersNum).forEach(n -> {
                ChunkRenderer chunkRendererToDestruct = idleChunks.pollFirst();
                assert chunkRendererToDestruct != null;
                chunkRendererToDestruct.delete();
            });
        }
    }
    
    private void dismissInactiveChunkRenderers() {
        assert Globals.useHackedChunkRenderDispatcher;
        
        long currentTime = System.nanoTime();
        final long deletingValve = 1000000000L * 30;//30 seconds
        //NOTE if you miss 'L' then it will overflow
        
        for (ChunkRenderer ChunkRenderer : this.renderers) {
            //make sure none of render chunk get dismissed
            updateLastUsedTime(ChunkRenderer);
        }
    
        ArrayDeque<ChunkRenderer> chunkRenderersToDismiss = lastActiveNanoTime.entrySet().stream()
            .filter(
                entry -> currentTime - entry.getValue() > deletingValve
            )
            .map(
                Map.Entry::getKey
            )
            .collect(Collectors.toCollection(ArrayDeque::new));
    
        if (!chunkRenderersToDismiss.isEmpty()) {
            Helper.log(String.format(
                "dismissed %d render chunks",
                chunkRenderersToDismiss.size()
            ));
        }
    
        chunkRenderersToDismiss.forEach(
            ChunkRenderer -> dismissChunkRenderer(ChunkRenderer.getOrigin())
        );
    }
    
    @Inject(
        method = "updateCameraPosition",
        at = @At("HEAD"),
        cancellable = true
    )
    private void updateCameraPosition(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        if (Globals.useHackedChunkRenderDispatcher) {
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
            
            ci.cancel();
        }
        else {
            if (Globals.portalRenderManager.isRendering()) {
                if (
                    MinecraftClient.getInstance().cameraEntity.dimension ==
                        Globals.portalRenderManager.getOriginalPlayerDimension()
                ) {
                    ci.cancel();
                }
            }
        }
    }
    
    //NOTE input block pos instead of chunk pos
    private ChunkRenderer myGetChunkRenderer(BlockPos blockPos) {
        assert Globals.useHackedChunkRenderDispatcher;
        
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
    
    private void updateLastUsedTime(ChunkRenderer chunkRenderer) {
        assert Globals.useHackedChunkRenderDispatcher;
        if (chunkRenderer == null) {
            return;
        }
        lastActiveNanoTime.put(chunkRenderer, System.nanoTime());
    }
    
    @Shadow
    public abstract int method_3328(int int_1, int int_2, int int_3);
    
    @Shadow
    public abstract int getChunkIndex(int int_1, int int_2, int int_3);
}
