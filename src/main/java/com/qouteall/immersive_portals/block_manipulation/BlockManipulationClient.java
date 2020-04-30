package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.commands.MyCommandServer;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.dimension.DimensionType;

public class BlockManipulationClient {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static DimensionType remotePointedDim;
    public static HitResult remoteHitResult;
    public static boolean isContextSwitched = false;
    
    public static boolean isPointingToPortal() {
        return remotePointedDim != null;
    }

    private static BlockHitResult createMissedHitResult(Vec3d from, Vec3d to) {
        Vec3d dir = to.subtract(from).normalize();

        return BlockHitResult.createMissed(to, Direction.getFacing(dir.x, dir.y, dir.z), new BlockPos(to));
    }

    private static boolean hitResultIsMissedOrNull(HitResult bhr) {
        return bhr == null || bhr.getType() == HitResult.Type.MISS;
    }

    public static void updatePointedBlock(float partialTicks) {
        if (client.interactionManager == null || client.world == null) {
            return;
        }
        
        remotePointedDim = null;
        remoteHitResult = null;
        
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        
        float reachDistance = client.interactionManager.getReachDistance();

        MyCommandServer.getPlayerPointingPortalRaw(
            client.player, partialTicks, reachDistance, true
        ).ifPresent(pair -> {
            if(pair.getFirst().isInteractable()) {
                double distanceToPortalPointing = pair.getSecond().distanceTo(cameraPos);
                if(distanceToPortalPointing < getCurrentTargetDistance() + 0.2) {
                    client.crosshairTarget = createMissedHitResult(cameraPos, pair.getSecond());

                    updateTargetedBlockThroughPortal(
                            cameraPos,
                            client.player.getRotationVec(partialTicks),
                            client.player.dimension,
                            distanceToPortalPointing,
                            reachDistance,
                            pair.getFirst()
                    );
                }
            }
        });
    }

    private static double getCurrentTargetDistance() {
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        if (hitResultIsMissedOrNull(client.crosshairTarget)) {
            return 23333;
        }

        //pointing to placeholder block does not count
        if (client.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) client.crosshairTarget;
            BlockPos hitPos = hitResult.getBlockPos();
            if (client.world.getBlockState(hitPos).getBlock() == PortalPlaceholderBlock.instance) {
                return 23333;
            }
        }
        
        return cameraPos.distanceTo(client.crosshairTarget.getPos());
    }
    
    private static void updateTargetedBlockThroughPortal(
        Vec3d cameraPos,
        Vec3d viewVector,
        DimensionType playerDimension,
        double beginDistance,
        double endDistance,
        Portal portal
    ) {
        
        Vec3d from = portal.transformPoint(
            cameraPos.add(viewVector.multiply(beginDistance))
        );
        Vec3d to = portal.transformPoint(
            cameraPos.add(viewVector.multiply(endDistance))
        );
        
        RayTraceContext context = new RayTraceContext(
            from,
            to,
            RayTraceContext.ShapeType.OUTLINE,
            RayTraceContext.FluidHandling.NONE,
            client.player
        );
        
        ClientWorld world = CGlobal.clientWorldLoader.getWorld(
            portal.dimensionTo
        );
        
        remoteHitResult = BlockView.rayTrace(
            context,
            (rayTraceContext, blockPos) -> {
                BlockState blockState = world.getBlockState(blockPos);
                
                //don't stop at placeholder block
                if (blockState.getBlock() == PortalPlaceholderBlock.instance) {
                    return null;
                }
                
                //when seeing through mirror don't stop at the glass block
                if (portal instanceof Mirror) {
                    if (portal.getDistanceToNearestPointInPortal(new Vec3d(blockPos).add(
                        0.5,
                        0.5,
                        0.5
                    )) < 0.6) {
                        return null;
                    }
                }
                
                FluidState fluidState = world.getFluidState(blockPos);
                Vec3d start = rayTraceContext.getStart();
                Vec3d end = rayTraceContext.getEnd();
                /**{@link VoxelShape#rayTrace(Vec3d, Vec3d, BlockPos)}*/
                //correct the start pos to avoid being considered inside block
                Vec3d correctedStart = start.subtract(end.subtract(start).multiply(0.0015));
                VoxelShape solidShape = rayTraceContext.getBlockShape(blockState, world, blockPos);
                BlockHitResult blockHitResult = world.rayTraceBlock(
                    correctedStart, end, blockPos, solidShape, blockState
                );
                VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, blockPos);
                BlockHitResult blockHitResult2 = fluidShape.rayTrace(start, end, blockPos);
                double d = blockHitResult == null ? Double.MAX_VALUE :
                    rayTraceContext.getStart().squaredDistanceTo(blockHitResult.getPos());
                double e = blockHitResult2 == null ? Double.MAX_VALUE :
                    rayTraceContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
                return d <= e ? blockHitResult : blockHitResult2;
            },
            (rayTraceContext) -> {
                Vec3d vec3d = rayTraceContext.getStart().subtract(rayTraceContext.getEnd());
                return BlockHitResult.createMissed(
                    rayTraceContext.getEnd(),
                    Direction.getFacing(vec3d.x, vec3d.y, vec3d.z),
                    new BlockPos(rayTraceContext.getEnd())
                );
            }
        );
        
        if (remoteHitResult.getPos().y < 0.1) {
            remoteHitResult = new BlockHitResult(
                remoteHitResult.getPos(),
                Direction.DOWN,
                ((BlockHitResult) remoteHitResult).getBlockPos(),
                ((BlockHitResult) remoteHitResult).isInsideBlock()
            );
        }
        
        if (remoteHitResult != null) {
            if (!world.getBlockState(((BlockHitResult) remoteHitResult).getBlockPos()).isAir()) {
                client.crosshairTarget = createMissedHitResult(from, to);
                remotePointedDim = portal.dimensionTo;
            }
        }
        
    }

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
                
            }
            else {
                client.interactionManager.cancelBlockBreaking();
            }
        }
    }
    
    //hacky switch
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
        }
        finally {
            client.world = oldWorld;
            isContextSwitched = false;
        }
        
    }
    
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
        }
        finally {
            client.world = oldWorld;
            isContextSwitched = false;
        }
        
        client.player.swingHand(Hand.MAIN_HAND);
    }
    
    //too lazy to rewrite the whole interaction system so hack there and here
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
                return;
            }
        }
    }
    
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
            client.player.world = targetWorld;
            client.world = targetWorld;
            isContextSwitched = true;
            
            return client.interactionManager.interactBlock(
                client.player, targetWorld, hand, blockHitResult
            );
        }
        finally {
            client.player.world = oldWorld;
            client.world = oldWorld;
            isContextSwitched = false;
        }
    }
    
}
