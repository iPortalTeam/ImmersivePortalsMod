package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockManipulationClient {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static List<Portal> remoteHitPortals = new ArrayList<>();
    public static DimensionType remotePointedDim;
    public static HitResult remoteHitResult;

    public static boolean isContextSwitched = false;

    public static boolean isPointingToPortal() {
        return !remoteHitPortals.isEmpty();
    }

    private static boolean hitResultIsMissedOrNull(HitResult bhr) {
        return bhr == null || bhr.getType() == HitResult.Type.MISS;
    }

    public static void updatePointedBlock(float partialTicks) {
        if (client.interactionManager == null || client.world == null || client.player == null) {
            return;
        }

        remoteHitPortals.clear();
        remotePointedDim = null;
        remoteHitResult = null;

        Vec3d start = client.gameRenderer.getCamera().getPos();
        float reachDistance = client.interactionManager.getReachDistance();
        Vec3d looking = client.player.getRotationVec(partialTicks);
        Vec3d end = start.add(looking.multiply(reachDistance));

        RayTraceContext rtc = new RayTraceContext(start, end, RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.NONE, client.player);
        Pair<BlockHitResult, List<Portal>> results = Helper.rayTrace(client.player.world, rtc, true);

        client.crosshairTarget = results.getLeft();

        remoteHitPortals.addAll(results.getRight());
        remotePointedDim = !remoteHitPortals.isEmpty() ? remoteHitPortals.get(remoteHitPortals.size() - 1).dimensionTo : null;
        remoteHitResult = remotePointedDim != null ? client.crosshairTarget : null;

        MyRenderHelper.debugText = results.getRight()
            .stream()
            .map(Portal::toString)
            .collect(Collectors.joining("\n"));
    }

    private static double getCurrentTargetDistance() {
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        if (hitResultIsMissedOrNull(client.crosshairTarget)) {
            return 23333;
        }

        return cameraPos.distanceTo(client.crosshairTarget.getPos());
    }

    @SuppressWarnings("LocalVariableDeclarationSideOnly")
    public static void myHandleBlockBreaking(boolean isKeyPressed) {
//        if (remoteHitResult == null) {
//            return;
//        }


        if (!client.player.isUsingItem()) {
            if (isKeyPressed && isPointingToPortal()) {
                BlockHitResult blockHitResult = (BlockHitResult) remoteHitResult;
                BlockPos blockPos = blockHitResult.getBlockPos();
                ClientWorld remoteWorld =
                    CGlobal.clientWorldLoader.getWorld(remotePointedDim);
                if (!remoteWorld.getBlockState(blockPos).isAir()) {
                    Direction direction = blockHitResult.getSide();
                    if (myUpdateBlockBreakingProgress(blockPos, direction)) {
                        client.particleManager.addBlockBreakingParticles(blockPos, direction);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }

            } else {
                client.interactionManager.cancelBlockBreaking();
            }
        }
    }

    //hacky switch
    @SuppressWarnings("LocalVariableDeclarationSideOnly")
    public static boolean myUpdateBlockBreakingProgress(
        BlockPos blockPos,
        Direction direction
    ) {
//        if (remoteHitResult == null) {
//            return false;
//        }

        ClientWorld oldWorld = client.world;
        client.world = CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        isContextSwitched = true;

        try {
            return client.interactionManager.updateBlockBreakingProgress(blockPos, direction);
        } finally {
            client.world = oldWorld;
            isContextSwitched = false;
        }

    }

    @SuppressWarnings("LocalVariableDeclarationSideOnly")
    public static void myAttackBlock() {
//        if (remoteHitResult == null) {
//            return;
//        }


        ClientWorld targetWorld =
            CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        BlockPos blockPos = ((BlockHitResult) remoteHitResult).getBlockPos();

        if (targetWorld.isAir(blockPos)) {
            return;
        }

        ClientWorld oldWorld = client.world;

        client.world = targetWorld;
        isContextSwitched = true;

        try {
            client.interactionManager.attackBlock(
                blockPos,
                ((BlockHitResult) remoteHitResult).getSide()
            );
        } finally {
            client.world = oldWorld;
            isContextSwitched = false;
        }

        client.player.swingHand(Hand.MAIN_HAND);
    }

    //too lazy to rewrite the whole interaction system so hack there and here
    @SuppressWarnings("LocalVariableDeclarationSideOnly")
    public static void myItemUse(Hand hand) {
//        if (remoteHitResult == null) {
//            return;
//        }

        ClientWorld targetWorld =
            CGlobal.clientWorldLoader.getWorld(remotePointedDim);

        ItemStack itemStack = client.player.getStackInHand(hand);
        BlockHitResult blockHitResult = (BlockHitResult) remoteHitResult;

        Pair<BlockHitResult, DimensionType> result =
            BlockManipulationServer.getHitResultForPlacing(targetWorld, blockHitResult);
        blockHitResult = result.getLeft();
        targetWorld = CGlobal.clientWorldLoader.getWorld(result.getRight());
        remoteHitResult = blockHitResult;
        remotePointedDim = result.getRight();

        int i = itemStack.getCount();
        ActionResult actionResult2 = myInteractBlock(hand, targetWorld, blockHitResult);
        if (actionResult2.isAccepted()) {
            if (actionResult2.shouldSwingHand()) {
                client.player.swingHand(hand);
                if (!itemStack.isEmpty() && (itemStack.getCount() != i || client.interactionManager.hasCreativeInventory())) {
                    client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                }
            }

            return;
        }

        if (actionResult2 == ActionResult.FAIL) {
            return;
        }

        if (!itemStack.isEmpty()) {
            ActionResult actionResult3 = client.interactionManager.interactItem(
                client.player,
                targetWorld,
                hand
            );
            if (actionResult3.isAccepted()) {
                if (actionResult3.shouldSwingHand()) {
                    client.player.swingHand(hand);
                }

                client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
            }
        }
    }

    @SuppressWarnings("LocalVariableDeclarationSideOnly")
    private static ActionResult myInteractBlock(
        Hand hand,
        ClientWorld targetWorld,
        BlockHitResult blockHitResult
    ) {
//        if (remoteHitResult == null) {
//            return null;
//        }

        ClientWorld oldWorld = client.world;

        try {
            //noinspection ConstantConditions
            client.player.world = targetWorld;
            client.world = targetWorld;
            isContextSwitched = true;

            return client.interactionManager.interactBlock(
                client.player, targetWorld, hand, blockHitResult
            );
        } finally {
            client.player.world = oldWorld;
            client.world = oldWorld;
            isContextSwitched = false;
        }
    }

}
