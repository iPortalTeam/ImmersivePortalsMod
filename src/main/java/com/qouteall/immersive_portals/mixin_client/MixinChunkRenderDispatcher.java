package com.qouteall.immersive_portals.mixin_client;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEChunkRenderDispatcher;
import com.qouteall.immersive_portals.render.RenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//NOTE WeakReference does not override equals()
//don't put them into set
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
    
    private Map<ChunkPos, ChunkRenderer[]> presetCache;
    private ChunkRendererFactory factory;
    private Map<BlockPos, ChunkRenderer> chunkRendererMap;
    private Deque<ChunkRenderer> idleChunks;
    private Set<ChunkRenderer[]> isNeighborUpdated;
    private WeakReference<ChunkRenderer[]> mainPreset;
    
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
        idleChunks = new ArrayDeque<>();
        
        presetCache = new HashMap<>();
        isNeighborUpdated = new HashSet<>();
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            ((IEChunkRenderDispatcher) this),
            IEChunkRenderDispatcher::tick
        );
        
        if (CGlobal.useHackedChunkRenderDispatcher) {
            //it will run createChunks() before this
            for (ChunkRenderer renderChunk : renderers) {
                chunkRendererMap.put(getOriginNonMutable(renderChunk), renderChunk);
            }
            updateNeighbours();
            fixAbnormality();
        }
    
        mainPreset = new WeakReference<>(null);
    }
    
    private BlockPos getOriginNonMutable(ChunkRenderer renderChunk) {
        return renderChunk.getOrigin().toImmutable();
    }
    
    @Inject(method = "delete", at = @At("HEAD"), cancellable = true)
    private void delete(CallbackInfo ci) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            chunkRendererMap.values().forEach(ChunkRenderer::delete);
            idleChunks.forEach(ChunkRenderer::delete);
            
            chunkRendererMap.clear();
            idleChunks.clear();
            
            presetCache.clear();
            isNeighborUpdated.clear();
            
            ci.cancel();
        }
    }
    
    @Override
    public void tick() {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ClientWorld worldClient = MinecraftClient.getInstance().world;
            if (worldClient != null) {
                if (worldClient.getTime() % 533 == 66) {
                    fixAbnormality();
                    dismissInactiveChunkRenderers();
                    presetCache.clear();
                    isNeighborUpdated.clear();
                }
            }
        }
    }
    
    private ChunkRenderer findAndEmployChunkRenderer(BlockPos basePos) {
        assert !chunkRendererMap.containsKey(basePos);
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        ChunkRenderer chunkRenderer = idleChunks.pollLast();
        
        if (chunkRenderer == null) {
            MinecraftClient.getInstance().getProfiler().push("create_chunk_renderer");
            chunkRenderer = factory.create(world, renderer);
            MinecraftClient.getInstance().getProfiler().pop();
        }
        
        employChunkRenderer(chunkRenderer, basePos);
        
        if (chunkRenderer.getWorld() == null) {
            Helper.err("Employed invalid chunk renderer");
        }
        
        return chunkRenderer;
    }
    
    private void employChunkRenderer(ChunkRenderer chunkRenderer, BlockPos basePos) {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        MinecraftClient.getInstance().getProfiler().push("employ");
        
        assert basePos.getX() % 16 == 0;
        assert basePos.getY() % 16 == 0;
        assert basePos.getZ() % 16 == 0;
        
        chunkRenderer.setOrigin(basePos.getX(), basePos.getY(), basePos.getZ());
        BlockPos origin = getOriginNonMutable(chunkRenderer);
        assert !chunkRendererMap.containsKey(origin);
        chunkRendererMap.put(origin, chunkRenderer);
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private void dismissChunkRenderer(BlockPos basePos) {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        ChunkRenderer chunkRenderer = chunkRendererMap.remove(basePos);
        
        if (chunkRenderer == null) {
            Helper.log("Chunk Renderer Abnormal");
            return;
        }
        
        idleChunks.addLast(chunkRenderer);
        
        destructAbundantIdleChunks();
    }
    
    private void destructAbundantIdleChunks() {
        assert CGlobal.useHackedChunkRenderDispatcher;
        if (idleChunks.size() > CGlobal.maxIdleChunkRendererNum) {
            int toDestructChunkRenderersNum = idleChunks.size() - CGlobal.maxIdleChunkRendererNum;
            IntStream.range(0, toDestructChunkRenderersNum).forEach(n -> {
                idleChunks.pollFirst().delete();
            });
        }
    }
    
    private void dismissInactiveChunkRenderers() {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        Set<ChunkRenderer> activeRenderers = Streams.concat(
            presetCache.values().stream().flatMap(
                Arrays::stream
            ),
            Arrays.stream(renderers)
        ).collect(Collectors.toSet());
        
        chunkRendererMap.values().stream()
            .filter(r -> !activeRenderers.contains(r))
            .collect(Collectors.toList())
            .forEach(
                chunkRenderer -> dismissChunkRenderer(getOriginNonMutable(chunkRenderer))
            );
    }
    
    @Inject(method = "updateCameraPosition", at = @At("HEAD"), cancellable = true)
    private void updateCameraPosition(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            MinecraftClient.getInstance().getProfiler().push(
                "update_hacked_chunk_render_dispatcher"
            );
            
            ChunkPos currPlayerChunkPos = new ChunkPos(
                (((int) viewEntityX) / 16),
                (((int) viewEntityZ) / 16)
            );
            renderers = presetCache.computeIfAbsent(
                currPlayerChunkPos,
                k -> createPreset(viewEntityX, viewEntityZ)
            );
    
            if (!CGlobal.renderer.isRendering()) {
                mainPreset = new WeakReference<>(renderers);
            }
            
            if (!isNeighborUpdated.contains(renderers)) {
                updateNeighbours();
                isNeighborUpdated.add(renderers);
                if (CGlobal.renderer.isRendering()) {
                    //the neighbor stuff is strange
                    isNeighborUpdated.remove(mainPreset.get());
                }
            }
            
            MinecraftClient.getInstance().getProfiler().pop();
            
            ci.cancel();
        }
        else {
            if (CGlobal.renderer.isRendering()) {
                if (
                    MinecraftClient.getInstance().cameraEntity.dimension ==
                        RenderHelper.originalPlayerDimension
                ) {
                    ci.cancel();
                }
            }
        }
    }
    
    //I can't figure out why its origin is incorrect sometimes
    //so fix it manually
    private void fixAbnormality() {
        if (chunkRendererMap.values().stream().distinct().count() != chunkRendererMap.size()) {
            Helper.err("Not distinct!");
        }

//        chunkRendererMap.entrySet().stream().filter(
//            entry -> {
//                boolean isPosCorrect = entry.getKey().equals(entry.getValue().getOrigin());
//                boolean isKeyMutable = entry.getKey() instanceof BlockPos.Mutable;
//                if (!isPosCorrect || isKeyMutable) {
//                    Helper.err("Chunk Renderer Abnormal " + entry.getKey() + entry.getValue().getOrigin());
//                    return true;
//                }
//                return false;
//            }
//        ).collect(
//            Collectors.toList()
//        ).forEach(entry -> {
//            entry.getValue().setOrigin(
//                entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()
//            );
//        });
    }
    
    private ChunkRenderer[] createPreset(double viewEntityX, double viewEntityZ) {
        ChunkRenderer[] preset = new ChunkRenderer[this.sizeX * this.sizeY * this.sizeZ];
        
        int px = MathHelper.floor(viewEntityX) - 8;
        int pz = MathHelper.floor(viewEntityZ) - 8;
        
        int maxLen = this.sizeX * 16;
        
        for (int cx = 0; cx < this.sizeX; ++cx) {
            int posX = this.method_3328(px, maxLen, cx);
            
            for (int cz = 0; cz < this.sizeZ; ++cz) {
                int posZ = this.method_3328(pz, maxLen, cz);
                
                for (int cy = 0; cy < this.sizeY; ++cy) {
                    int posY = cy * 16;
                    
                    preset[this.getChunkIndex(cx, cy, cz)] =
                        validateChunkRenderer(
                            myGetChunkRenderer(
                                new BlockPos(posX, posY, posZ)
                            )
                        );
                }
            }
        }
        
        return preset;
    }
    
    private void updateNeighbours() {
        if (!CGlobal.isOptifinePresent) {
            return;
        }
        
        MinecraftClient.getInstance().getProfiler().push("neighbor");
        
        for (int j = 0; j < this.renderers.length; ++j) {
            ChunkRenderer renderChunk = this.renderers[j];
            
            for (int l = 0; l < Direction.ALL.length; ++l) {
                Direction facing = Direction.ALL[l];
                BlockPos posOffset16 = renderChunk.getNeighborPosition(facing);
                ChunkRenderer neighbour = getChunkRenderer(posOffset16);
                renderChunk.setRenderChunkNeighbour(facing, neighbour);
            }
        }
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    //NOTE input block pos instead of chunk pos
    private ChunkRenderer myGetChunkRenderer(BlockPos blockPos) {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        BlockPos basePos = getBasePos(blockPos);
        
        if (chunkRendererMap.containsKey(basePos)) {
            return chunkRendererMap.get(basePos);
        }
        else {
            return findAndEmployChunkRenderer(basePos);
        }
    }
    
    private static BlockPos getBasePos(BlockPos blockPos) {
        return new BlockPos(
            MathHelper.floorDiv(blockPos.getX(), 16) * 16,
            MathHelper.floorDiv(blockPos.getY(), 16) * 16,
            MathHelper.floorDiv(blockPos.getZ(), 16) * 16
        );
    }
    
    @Shadow
    public abstract int method_3328(int int_1, int int_2, int int_3);
    
    @Shadow
    public abstract int getChunkIndex(int int_1, int int_2, int int_3);
    
    @Shadow
    public abstract ChunkRenderer getChunkRenderer(BlockPos pos);
    
    @Override
    public int getEmployedRendererNum() {
        return CGlobal.useHackedChunkRenderDispatcher ? chunkRendererMap.size() : renderers.length;
    }
    
    @Override
    public void rebuildAll() {
        for (ChunkRenderer chunkRenderer : renderers) {
            chunkRenderer.scheduleRebuild(true);
        }
    }
    
    private ChunkRenderer validateChunkRenderer(ChunkRenderer chunkRenderer) {
        if (chunkRenderer.getWorld() == null) {
            Helper.err("Invalid Chunk Renderer " +
                world.dimension.getType() +
                getOriginNonMutable(chunkRenderer));
            return findAndEmployChunkRenderer(getOriginNonMutable(chunkRenderer));
        }
        else {
            return chunkRenderer;
        }
    }
}
