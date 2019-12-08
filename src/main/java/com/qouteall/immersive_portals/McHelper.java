package com.qouteall.immersive_portals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class McHelper {
    
    public static IEThreadedAnvilChunkStorage getIEStorage(DimensionType dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkManager) getServer().getWorld(dimension).getChunkManager()
        ).threadedAnvilChunkStorage;
    }
    
    public static ArrayList<ServerPlayerEntity> getCopiedPlayerList() {
        return new ArrayList<>(getServer().getPlayerManager().getPlayerList());
    }
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
    }
    
    public static MinecraftServer getServer() {
        return Helper.refMinecraftServer.get();
    }
    
    public static <MSG> void sendToServer(MSG message) {
        assert false;
    }
    
    public static <MSG> void sendToPlayer(ServerPlayerEntity player, MSG message) {
        assert false;
    }
    
    public static ServerWorld getOverWorldOnServer() {
        return getServer().getWorld(DimensionType.OVERWORLD);
    }
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        //Helper.log(text);
        
        player.sendMessage(new LiteralText(text));
    }
    
    public static Box getChunkBoundingBox(ChunkPos chunkPos) {
        return new Box(
            chunkPos.getCenterBlockPos(),
            chunkPos.getCenterBlockPos().add(16, 256, 16)
        );
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getTime();
    }
    
    public static <T> void performSplitedFindingTaskOnServer(
        Iterator<T> iterator,
        Predicate<T> predicate,
        IntPredicate progressInformer,//return false to abort the task
        Consumer<T> onFound,
        Runnable onNotFound
    ) {
        final long timeValve = (1000000000L / 40);
        int[] countStorage = new int[1];
        countStorage[0] = 0;
        ModMain.serverTaskList.addTask(() -> {
            boolean shouldContinueRunning =
                progressInformer.test(countStorage[0]);
            if (!shouldContinueRunning) {
                return true;
            }
            long startTime = System.nanoTime();
            for (; ; ) {
                if (iterator.hasNext()) {
                    T next = iterator.next();
                    if (predicate.test(next)) {
                        onFound.accept(next);
                        return true;
                    }
                }
                else {
                    //finished searching
                    onNotFound.run();
                    return true;
                }
                countStorage[0] += 1;
                
                long currTime = System.nanoTime();
                
                if (currTime - startTime > timeValve) {
                    //suspend the task and retry it next tick
                    return false;
                }
            }
        });
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        Box box = new Box(center, center).expand(range);
        return (Stream) world.getEntities(entityClass, box, e -> true).stream();
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        Entity center,
        Class<ENTITY> entityClass,
        double range
    ) {
        return getEntitiesNearby(
            center.world,
            center.getPos(),
            entityClass,
            range
        );
    }
    
    public static void renderWithTransformation(
        MatrixStack matrixStack,
        Runnable renderingFunc
    ) {
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(matrixStack.peek().getModel());
        renderingFunc.run();
        RenderSystem.popMatrix();
    }
}
