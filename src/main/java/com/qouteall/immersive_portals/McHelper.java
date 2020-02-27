package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
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
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    
    public static List<ServerPlayerEntity> getRawPlayerList() {
        return getServer().getPlayerManager().getPlayerList();
    }
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
    }
    
    public static MinecraftServer getServer() {
        return Helper.refMinecraftServer.get();
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
        final long timeValve = (1000000000L / 50);
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
    
    public static void runWithTransformation(
        MatrixStack matrixStack,
        Runnable renderingFunc
    ) {
        transformationPush(matrixStack);
        renderingFunc.run();
        transformationPop();
    }
    
    public static void transformationPop() {
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();
    }
    
    public static void transformationPush(MatrixStack matrixStack) {
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(matrixStack.peek().getModel());
    }
    
    public static List<GlobalTrackedPortal> getGlobalPortals(World world) {
        if (world.isClient) {
            return CHelper.getClientGlobalPortal(world);
        }
        else {
            return GlobalPortalStorage.get(((ServerWorld) world)).data;
        }
    }
    
    public static Stream<Portal> getServerPortalsNearby(Entity center, double range) {
        List<GlobalTrackedPortal> globalPortals = GlobalPortalStorage.get(((ServerWorld) center.world)).data;
        Stream<Portal> nearbyPortals = McHelper.getEntitiesNearby(
            center,
            Portal.class,
            range
        );
        if (globalPortals == null) {
            return nearbyPortals;
        }
        else {
            return Streams.concat(
                globalPortals.stream().filter(
                    p -> p.getDistanceToNearestPointInPortal(center.getPos()) < range * 2
                ),
                nearbyPortals
            );
        }
    }
    
    public static int getRenderDistanceOnServer() {
        return getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3d pos,
        Vec3d lastTickPos
    ) {
    
    
        //NOTE do not call entity.setPosition() because it may tick the entity
        entity.setPos(pos.x, pos.y, pos.z);
        entity.lastRenderX = lastTickPos.x;
        entity.lastRenderY = lastTickPos.y;
        entity.lastRenderZ = lastTickPos.z;
        entity.prevX = lastTickPos.x;
        entity.prevY = lastTickPos.y;
        entity.prevZ = lastTickPos.z;
    }
    
    public static double getVehicleY(Entity vehicle, Entity passenger) {
        return passenger.getY() - vehicle.getMountedHeightOffset() - passenger.getHeightOffset();
    }
    
    public static void adjustVehicle(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return;
        }
    
        vehicle.updatePosition(
            entity.getX(),
            getVehicleY(vehicle, entity),
            entity.getZ()
        );
    }
    
    public static void checkDimension(Entity entity) {
        if (entity.dimension != entity.world.dimension.getType()) {
            Helper.err(String.format(
                "Entity dimension field abnormal. Force corrected. %s %s %s",
                entity,
                entity.dimension,
                entity.world.dimension.getType()
            ));
            entity.dimension = entity.world.dimension.getType();
        }
    }
}
