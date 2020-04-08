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
    public static DimensionType remotePointedDim;
    public static HitResult remoteHitResult;
    public static boolean isContextSwitched = false;
    
    public static boolean isPointingToRemoteBlock() {
        return remotePointedDim != null;
    }
    
    public static void onPointedBlockUpdated(float partialTicks) {
        remotePointedDim = null;
        remoteHitResult = null;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        
        float reachDistance = mc.interactionManager.getReachDistance();
        
        MyCommandServer.getPlayerPointingPortalRaw(
            mc.player, partialTicks, reachDistance, true
        ).ifPresent(pair -> {
            double distanceToPortalPointing = pair.getSecond().distanceTo(cameraPos);
            if (distanceToPortalPointing < getCurrentTargetDistance() + 0.2) {
                updateTargetedBlockThroughPortal(
                    cameraPos,
                    mc.player.getRotationVec(partialTicks),
                    mc.player.dimension,
                    distanceToPortalPointing,
                    reachDistance,
                    pair.getFirst()
                );
            }
        });
    }
    
    private static double getCurrentTargetDistance() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        
        if (mc.crosshairTarget == null) {
            return 23333;
        }
        
        //pointing to placeholder block does not count
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos hitPos = hitResult.getBlockPos();
            if (mc.world.getBlockState(hitPos).getBlock() == PortalPlaceholderBlock.instance) {
                return 23333;
            }
        }
        
        return cameraPos.distanceTo(mc.crosshairTarget.getPos());
    }
    
    private static void updateTargetedBlockThroughPortal(
        Vec3d cameraPos,
        Vec3d viewVector,
        DimensionType playerDimension,
        double beginDistance,
        double endDistance,
        Portal portal
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
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
            mc.player
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
                    if (portal.getDistanceToNearestPointInPortal(new Vec3d(blockPos).add(0.5,0.5,0.5)) < 0.6) {
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
                mc.crosshairTarget = null;
                remotePointedDim = portal.dimensionTo;
            }
        }
    }
    
    public static void myHandleBlockBreaking(boolean isKeyPressed) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        if (!mc.player.isUsingItem()) {
            if (isKeyPressed && isPointingToRemoteBlock()) {
                BlockHitResult blockHitResult = (BlockHitResult) remoteHitResult;
                BlockPos blockPos = blockHitResult.getBlockPos();
                ClientWorld remoteWorld =
                    CGlobal.clientWorldLoader.getWorld(remotePointedDim);
                if (!remoteWorld.getBlockState(blockPos).isAir()) {
                    Direction direction = blockHitResult.getSide();
                    if (myUpdateBlockBreakingProgress(mc, blockPos, direction)) {
                        mc.particleManager.addBlockBreakingParticles(blockPos, direction);
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }
                
            }
            else {
                mc.interactionManager.cancelBlockBreaking();
            }
        }
    }
    
    //hacky switch
    public static boolean myUpdateBlockBreakingProgress(
        MinecraftClient mc,
        BlockPos blockPos,
        Direction direction
    ) {
        ClientWorld oldWorld = mc.world;
        mc.world = CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        isContextSwitched = true;
        
        try {
            return mc.interactionManager.updateBlockBreakingProgress(blockPos, direction);
        }
        finally {
            mc.world = oldWorld;
            isContextSwitched = false;
        }
        
    }
    
    public static void myAttackBlock() {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        ClientWorld targetWorld =
            CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        BlockPos blockPos = ((BlockHitResult) remoteHitResult).getBlockPos();
        
        if (targetWorld.isAir(blockPos)) {
            return;
        }
        
        ClientWorld oldWorld = mc.world;
        
        mc.world = targetWorld;
        isContextSwitched = true;
        
        try {
            mc.interactionManager.attackBlock(
                blockPos,
                ((BlockHitResult) remoteHitResult).getSide()
            );
        }
        finally {
            mc.world = oldWorld;
            isContextSwitched = false;
        }
        
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    
    //too lazy to rewrite the whole interaction system so hack there and here
    public static void myItemUse(Hand hand) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld targetWorld =
            CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        
        ItemStack itemStack = mc.player.getStackInHand(hand);
        BlockHitResult blockHitResult = (BlockHitResult) remoteHitResult;
        
        Pair<BlockHitResult, DimensionType> result =
            BlockManipulationServer.getHitResultForPlacing(targetWorld, blockHitResult);
        blockHitResult = result.getLeft();
        targetWorld = CGlobal.clientWorldLoader.getWorld(result.getRight());
        remoteHitResult = blockHitResult;
        remotePointedDim = result.getRight();
        
        int i = itemStack.getCount();
        ActionResult actionResult2 = myInteractBlock(hand, mc, targetWorld, blockHitResult);
        if (actionResult2.isAccepted()) {
            if (actionResult2.shouldSwingHand()) {
                mc.player.swingHand(hand);
                if (!itemStack.isEmpty() && (itemStack.getCount() != i || mc.interactionManager.hasCreativeInventory())) {
                    mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                }
            }
            
            return;
        }
        
        if (actionResult2 == ActionResult.FAIL) {
            return;
        }
        
        if (!itemStack.isEmpty()) {
            ActionResult actionResult3 = mc.interactionManager.interactItem(
                mc.player,
                targetWorld,
                hand
            );
            if (actionResult3.isAccepted()) {
                if (actionResult3.shouldSwingHand()) {
                    mc.player.swingHand(hand);
                }
                
                mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                return;
            }
        }
    }
    
    private static ActionResult myInteractBlock(
        Hand hand,
        MinecraftClient mc,
        ClientWorld targetWorld,
        BlockHitResult blockHitResult
    ) {
        ClientWorld oldWorld = mc.world;
        
        try {
            mc.player.world = targetWorld;
            mc.world = targetWorld;
            isContextSwitched = true;
            
            return mc.interactionManager.interactBlock(
                mc.player, targetWorld, hand, blockHitResult
            );
        }
        finally {
            mc.player.world = oldWorld;
            mc.world = oldWorld;
            isContextSwitched = false;
        }
    }
    
}
