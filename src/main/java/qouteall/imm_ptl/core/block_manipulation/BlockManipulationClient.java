package qouteall.imm_ptl.core.block_manipulation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
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
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.PortalUtils;

import java.util.function.Supplier;

public class BlockManipulationClient {
    private static final Minecraft client = Minecraft.getInstance();
    
    public static ResourceKey<Level> remotePointedDim;
    public static HitResult remoteHitResult;
    
    public static boolean isPointingToPortal() {
        return remotePointedDim != null;
    }
    
    @Nullable
    public static ClientLevel getRemotePointedWorld() {
        if (remotePointedDim == null) {
            return null;
        }
        return ClientWorldLoader.getWorld(BlockManipulationClient.remotePointedDim);
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
        
        if (!BlockManipulationServer.canDoCrossPortalInteractionEvent.invoker().test(client.player)) {
            return;
        }
        
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
                    client.player.level().dimension(),
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
    
    /**
     * It will not switch the dimension of client player
     */
    public static <T> T withSwitchedContext(
        Supplier<T> func, boolean transformHitResult
    ) {
        Validate.notNull(remoteHitResult);
        
        ClientLevel remoteWorld = getRemotePointedWorld();
        Validate.notNull(remoteWorld);
        
        HitResult effectiveHitResult;
        
        if (transformHitResult && (remoteHitResult instanceof BlockHitResult blockHitResult)) {
            Tuple<BlockHitResult, ResourceKey<Level>> r =
                BlockManipulationServer.getHitResultForPlacing(remoteWorld, blockHitResult);
            effectiveHitResult = r.getA();
            remoteWorld = ClientWorldLoader.getWorld(r.getB());
            Validate.notNull(remoteWorld);
            Validate.notNull(effectiveHitResult);
        }
        else {
            effectiveHitResult = remoteHitResult;
        }
        
        return ClientWorldLoader.withSwitchedWorld(
            remoteWorld, () -> {
                HitResult originalHitResult = client.hitResult;
                client.hitResult = effectiveHitResult;
                try {
                    return func.get();
                }
                finally {
                    client.hitResult = originalHitResult;
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
