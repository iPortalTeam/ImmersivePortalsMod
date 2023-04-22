package qouteall.imm_ptl.core.block_manipulation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.PortalUtils;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockManipulationClient {
    private static final Minecraft client = Minecraft.getInstance();
    
    public static ResourceKey<Level> remotePointedDim;
    public static HitResult remoteHitResult;
    public static boolean isContextSwitched = false;
    
    public static boolean isPointingToPortal() {
        return remotePointedDim != null;
    }
    
    private static BlockHitResult createMissedHitResult(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        
        return BlockHitResult.miss(to, Direction.getNearest(dir.x, dir.y, dir.z), BlockPos.containing(to));
    }
    
    private static boolean hitResultIsMissedOrNull(HitResult bhr) {
        return bhr == null || bhr.getType() == HitResult.Type.MISS;
    }
    
    public static void updatePointedBlock(float tickDelta) {
        if (client.gameMode == null || client.level == null || client.player == null) {
            return;
        }
        
        remotePointedDim = null;
        remoteHitResult = null;
        
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        
        float reachDistance = client.gameMode.getPickRange();
    
        PortalUtils.raytracePortalFromEntityView(client.player, tickDelta, reachDistance, true, portal1 -> portal1.isInteractableBy(client.player)).ifPresent(pair -> {
            Portal portal = pair.getFirst();
            double distanceToPortalPointing = pair.getSecond().distanceTo(cameraPos);
            if (distanceToPortalPointing < getCurrentTargetDistance() + 0.2) {
                client.hitResult = createMissedHitResult(cameraPos, pair.getSecond());
                
                updateTargetedBlockThroughPortal(
                    cameraPos,
                    client.player.getViewVector(tickDelta),
                    client.player.level.dimension(),
                    distanceToPortalPointing,
                    reachDistance,
                    portal
                );
            }
        });
    }
    
    private static double getCurrentTargetDistance() {
        Vec3 cameraPos = client.gameRenderer.getMainCamera().getPosition();
        
        if (hitResultIsMissedOrNull(client.hitResult)) {
            return 23333;
        }
        
        if (client.hitResult instanceof BlockHitResult) {
            BlockPos hitPos = ((BlockHitResult) client.hitResult).getBlockPos();
            if (client.level.getBlockState(hitPos).getBlock() == PortalPlaceholderBlock.instance) {
                return 23333;
            }
        }
        
        return cameraPos.distanceTo(client.hitResult.getLocation());
    }
    
    private static void updateTargetedBlockThroughPortal(
        Vec3 cameraPos,
        Vec3 viewVector,
        ResourceKey<Level> playerDimension,
        double beginDistance,
        double endDistance,
        Portal portal
    ) {
        
        Vec3 from = portal.transformPoint(
            cameraPos.add(viewVector.scale(beginDistance))
        );
        Vec3 to = portal.transformPoint(
            cameraPos.add(viewVector.scale(endDistance))
        );
        
        //do not touch barrier block through world wrapping portal
//        from = from.add(to.subtract(from).normalize().multiply(0.00151));
        
        ClipContext context = new ClipContext(
            from,
            to,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            client.player
        );
        
        ClientLevel world = ClientWorldLoader.getWorld(portal.dimensionTo);
        
        remoteHitResult = BlockGetter.traverseBlocks(
            from, to,
            context,
            (rayTraceContext, blockPos) -> {
                BlockState blockState = world.getBlockState(blockPos);
                
                if (blockState.getBlock() == PortalPlaceholderBlock.instance) {
                    return null;
                }
                if (blockState.getBlock() == Blocks.BARRIER) {
                    return null;
                }
                
                FluidState fluidState = world.getFluidState(blockPos);
                Vec3 start = rayTraceContext.getFrom();
                Vec3 end = rayTraceContext.getTo();
                
                //correct the start pos to avoid being considered inside block
                Vec3 correctedStart = start.subtract(end.subtract(start).scale(0.0015));
//                Vec3d correctedStart = start;
                VoxelShape solidShape = rayTraceContext.getBlockShape(blockState, world, blockPos);
                BlockHitResult blockHitResult = world.clipWithInteractionOverride(
                    correctedStart, end, blockPos, solidShape, blockState
                );
                VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, blockPos);
                BlockHitResult fluidHitResult = fluidShape.clip(start, end, blockPos);
                double d = blockHitResult == null ? Double.MAX_VALUE :
                    rayTraceContext.getFrom().distanceToSqr(blockHitResult.getLocation());
                double e = fluidHitResult == null ? Double.MAX_VALUE :
                    rayTraceContext.getFrom().distanceToSqr(fluidHitResult.getLocation());
                return d <= e ? blockHitResult : fluidHitResult;
            },
            (rayTraceContext) -> {
                Vec3 vec3d = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
                return BlockHitResult.miss(
                    rayTraceContext.getTo(),
                    Direction.getNearest(vec3d.x, vec3d.y, vec3d.z),
                    BlockPos.containing(rayTraceContext.getTo())
                );
            }
        );
        
        if (remoteHitResult.getLocation().y < world.getMinBuildHeight() + 0.1) {
            remoteHitResult = new BlockHitResult(
                remoteHitResult.getLocation(),
                Direction.DOWN,
                ((BlockHitResult) remoteHitResult).getBlockPos(),
                ((BlockHitResult) remoteHitResult).isInside()
            );
        }
        
        if (remoteHitResult != null) {
            if (!world.getBlockState(((BlockHitResult) remoteHitResult).getBlockPos()).isAir()) {
                client.hitResult = createMissedHitResult(from, to);
                remotePointedDim = portal.dimensionTo;
            }
        }
        
    }
    
    public static void myHandleBlockBreaking(boolean isKeyPressed) {
        
        if (!client.player.isUsingItem()) {
            if (isKeyPressed && isPointingToPortal()) {
                BlockHitResult blockHitResult = (BlockHitResult) remoteHitResult;
                BlockPos blockPos = blockHitResult.getBlockPos();
                ClientLevel remoteWorld =
                    ClientWorldLoader.getWorld(remotePointedDim);
                if (!remoteWorld.getBlockState(blockPos).isAir()) {
                    Direction direction = blockHitResult.getDirection();
                    if (myUpdateBlockBreakingProgress(blockPos, direction)) {
                        client.particleEngine.crack(blockPos, direction);
                        client.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
                
            }
            else {
                client.gameMode.stopDestroyBlock();
            }
        }
    }
    
    public static boolean myUpdateBlockBreakingProgress(
        BlockPos blockPos,
        Direction direction
    ) {
        ClientLevel targetWorld = ClientWorldLoader.getWorld(remotePointedDim);
        
        return withWorldSwitchedAndPlayerDimensionSwitched(
            targetWorld,
            () -> {
                return client.gameMode.continueDestroyBlock(blockPos, direction);
            }
        );
    }
    
    /**
     * {@link Minecraft#startAttack()}
     */
    public static boolean myAttackBlock() {
        ClientLevel targetWorld =
            ClientWorldLoader.getWorld(remotePointedDim);
        BlockPos blockPos = ((BlockHitResult) remoteHitResult).getBlockPos();
        
        if (targetWorld.isEmptyBlock(blockPos)) {
            return true;
        }
        
        return withWorldSwitchedAndPlayerDimensionSwitched(
            targetWorld,
            () -> {
                client.gameMode.startDestroyBlock(
                    blockPos,
                    ((BlockHitResult) remoteHitResult).getDirection()
                );
                client.player.swing(InteractionHand.MAIN_HAND);
                return client.level.getBlockState(blockPos).isAir();
            }
        );
    }
    
    public static void myItemUse(InteractionHand hand) {
        ClientLevel targetWorld =
            ClientWorldLoader.getWorld(remotePointedDim);
        
        ItemStack itemStack = client.player.getItemInHand(hand);
        BlockHitResult blockHitResult = (BlockHitResult) remoteHitResult;
        
        Tuple<BlockHitResult, ResourceKey<Level>> result =
            BlockManipulationServer.getHitResultForPlacing(targetWorld, blockHitResult);
        blockHitResult = result.getA();
        targetWorld = ClientWorldLoader.getWorld(result.getB());
        remoteHitResult = blockHitResult;
        remotePointedDim = result.getB();
        
        int count = itemStack.getCount();
        InteractionResult actionResult2 = myInteractBlock(hand, targetWorld, blockHitResult);
        if (actionResult2.consumesAction()) {
            if (actionResult2.shouldSwing()) {
                client.player.swing(hand);
                if (!itemStack.isEmpty() && (itemStack.getCount() != count || client.gameMode.hasInfiniteItems())) {
                    client.gameRenderer.itemInHandRenderer.itemUsed(hand);
                }
            }
            
            return;
        }
        
        if (actionResult2 == InteractionResult.FAIL) {
            return;
        }
        
        if (!itemStack.isEmpty()) {
            InteractionResult actionResult3 = withWorldSwitchedAndPlayerDimensionSwitched(
                targetWorld,
                () -> client.gameMode.useItem(
                    client.player,
                    hand
                )
            );
            if (actionResult3.consumesAction()) {
                if (actionResult3.shouldSwing()) {
                    client.player.swing(hand);
                }
                
                client.gameRenderer.itemInHandRenderer.itemUsed(hand);
                return;
            }
        }
    }
    
    private static InteractionResult myInteractBlock(
        InteractionHand hand,
        ClientLevel targetWorld,
        BlockHitResult blockHitResult
    ) {
        return withWorldSwitchedAndPlayerDimensionSwitched(
            targetWorld,
            () -> client.gameMode.useItemOn(
                client.player, hand, blockHitResult
            )
        );
    }
    
//    /**
//     * In {@link MultiPlayerGameMode#performUseItemOn(LocalPlayer, InteractionHand, BlockHitResult)}
//     * {@link UseOnContext#UseOnContext(Player, InteractionHand, BlockHitResult)}
//     * It will use the player's current dimension which may be wrong when interacting another dimension's block
//     * So also switch the player's dimension
//     */
    private static <T> T withWorldSwitchedAndPlayerDimensionSwitched(
        ClientLevel targetWorld,
        Supplier<T> supplier
    ) {
        return IPMcHelper.withSwitchedContext(
            targetWorld,
            () -> {
                Level oldWorld = client.player.level;
                
                isContextSwitched = true;
                client.player.level = targetWorld;
                try {
                    return supplier.get();
                }
                finally {
                    isContextSwitched = false;
                    client.player.level = oldWorld;
                }
            }
        );
    }
    
    @Nullable
    public static String getDebugString() {
        if (remotePointedDim == null) {
            return null;
        }
        if (remoteHitResult instanceof BlockHitResult blockHitResult) {
            return "Point:%s %d %d %d".formatted(
                remotePointedDim.location(),
                blockHitResult.getBlockPos().getX(),
                blockHitResult.getBlockPos().getY(),
                blockHitResult.getBlockPos().getZ()
            );
        }
        else {
            return null;
        }
    }
}
