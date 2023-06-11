package qouteall.imm_ptl.core.block_manipulation;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class BlockManipulationServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final ThreadLocal<ServerLevel> SERVER_PLAYER_INTERACTION_REDIRECT =
        ThreadLocal.withInitial(() -> null);
    
    /**
     * Use this event to conditionally disable cross portal block interaction.
     * The result will be ANDed.
     */
    public static final Event<Predicate<Player>> canDoCrossPortalInteractionEvent =
        EventFactory.createArrayBacked(Predicate.class,
            handlers -> player -> {
                for (Predicate<Player> handler : handlers) {
                    if (!handler.test(player)) {
                        return false;
                    }
                }
                return true;
            });
    
    private static boolean canPlayerReach(
        ResourceKey<Level> dimension,
        ServerPlayer player,
        BlockPos requestPos
    ) {
        if (!canDoCrossPortalInteractionEvent.invoker().test(player)) {
            return false;
        }
        
        Float playerScale = PehkuiInterface.invoker.computeBlockReachScale(player);
        
        Vec3 pos = Vec3.atCenterOf(requestPos);
        Vec3 playerPos = player.position();
        double distanceSquare = 6 * 6 * 4 * 4 * playerScale * playerScale;
        if (player.level().dimension() == dimension) {
            if (playerPos.distanceToSqr(pos) < distanceSquare) {
                return true;
            }
        }
        return IPMcHelper.getNearbyPortals(
            player,
            IPGlobal.maxNormalPortalRadius
        ).anyMatch(portal ->
            portal.dimensionTo == dimension &&
                portal.isInteractableBy(player) &&
                portal.transformPoint(playerPos).distanceToSqr(pos) <
                    distanceSquare * portal.getScale() * portal.getScale()
        );
    }
    
    public static Tuple<BlockHitResult, ResourceKey<Level>> getHitResultForPlacing(
        Level world,
        BlockHitResult blockHitResult
    ) {
        Direction side = blockHitResult.getDirection();
        Vec3 sideVec = Vec3.atLowerCornerOf(side.getNormal());
        Vec3 hitCenter = Vec3.atCenterOf(blockHitResult.getBlockPos());
        
        List<Portal> globalPortals = GlobalPortalStorage.getGlobalPortals(world);
        
        Portal portal = globalPortals.stream().filter(p ->
            p.getNormal().dot(sideVec) < -0.9
                && p.isPointInPortalProjection(hitCenter)
                && p.getDistanceToPlane(hitCenter) < 0.6
        ).findFirst().orElse(null);
        
        if (portal == null) {
            return new Tuple<>(blockHitResult, world.dimension());
        }
        
        Vec3 newCenter = portal.transformPoint(hitCenter.add(sideVec.scale(0.501)));
        BlockPos placingBlockPos = BlockPos.containing(newCenter);
        
        BlockHitResult newHitResult = new BlockHitResult(
            Vec3.ZERO,
            side.getOpposite(),
            placingBlockPos,
            blockHitResult.isInside()
        );
        
        return new Tuple<>(newHitResult, portal.dimensionTo);
    }
    
    public static class RemoteCallables {
        /**
         * {@link qouteall.imm_ptl.core.mixin.client.interaction.MixinMultiPlayerGameMode}
         */
        public static void processPlayerActionPacket(
            ServerPlayer player,
            ResourceKey<Level> dimension,
            byte[] packetBytes
        ) {
            FriendlyByteBuf buf = IPMcHelper.bytesToBuf(packetBytes);
            ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(buf);
            
            ServerLevel world = MiscHelper.getServer().getLevel(dimension);
            Validate.notNull(world);
            
            withRedirect(
                world,
                () -> {
                    doProcessPlayerAction(world, player, packet);
                }
            );
        }
        
        public static void processUseItemOnPacket(
            ServerPlayer player,
            ResourceKey<Level> dimension,
            byte[] packetBytes
        ) {
            FriendlyByteBuf buf = IPMcHelper.bytesToBuf(packetBytes);
            ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(buf);
            
            ServerLevel world = MiscHelper.getServer().getLevel(dimension);
            Validate.notNull(world);
            
            withRedirect(
                world,
                () -> {
                    doProcessUseItemOn(world, player, packet);
                }
            );
        }
    }
    
    public static void init() {
    
    }
    
    private static void withRedirect(
        ServerLevel world,
        Runnable runnable
    ) {
        ServerLevel original = SERVER_PLAYER_INTERACTION_REDIRECT.get();
        SERVER_PLAYER_INTERACTION_REDIRECT.set(world);
        try {
            PacketRedirection.withForceRedirect(
                world, runnable
            );
        }
        finally {
            SERVER_PLAYER_INTERACTION_REDIRECT.set(original);
        }
    }
    
    /**
     * {@link ServerGamePacketListenerImpl#handlePlayerAction(ServerboundPlayerActionPacket)}
     */
    @IPVanillaCopy
    private static void doProcessPlayerAction(ServerLevel world, ServerPlayer player, ServerboundPlayerActionPacket packet) {
        player.resetLastActionTime();
        BlockPos blockPos = packet.getPos();
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        
        if (!canPlayerReach(world.dimension(), player, blockPos)) {
            LOGGER.error("Reject cross-portal action {} {} {}", player, world, blockPos);
            return;
        }
        
        if (isAttackingAction(action)) {
            player.gameMode.handleBlockBreakAction(
                blockPos, action, packet.getDirection(),
                player.level().getMaxBuildHeight(), packet.getSequence()
            );
            player.connection.ackBlockChangesUpTo(packet.getSequence());
        }
    }
    
    public static boolean isAttackingAction(ServerboundPlayerActionPacket.Action action) {
        return action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK ||
            action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
            action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
    }
    
    /**
     * {@link ServerGamePacketListenerImpl#handleUseItemOn(ServerboundUseItemOnPacket)}
     */
    @IPVanillaCopy
    private static void doProcessUseItemOn(
        ServerLevel world, ServerPlayer player, ServerboundUseItemOnPacket packet
    ) {
        player.connection.ackBlockChangesUpTo(packet.getSequence());
        InteractionHand hand = packet.getHand();
        BlockHitResult blockHitResult = packet.getHitResult();
        ResourceKey<Level> dimension = world.dimension();
        
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!itemStack.isItemEnabled(world.enabledFeatures())) {
            return;
        }
        
        BlockPos blockPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getDirection();
        player.resetLastActionTime();
        if (world.mayInteract(player, blockPos)) {
            if (!canPlayerReach(dimension, player, blockPos)) {
                LOGGER.error("Reject cross-portal action {} {} {}", player, world, blockPos);
                return;
            }
            
            InteractionResult actionResult = player.gameMode.useItemOn(
                player,
                world,
                itemStack,
                hand,
                blockHitResult
            );
            if (actionResult.shouldSwing()) {
                player.swing(hand, true);
            }
        }
        
        PacketRedirection.sendRedirectedMessage(
            player,
            dimension,
            new ClientboundBlockUpdatePacket(world, blockPos)
        );
        
        BlockPos offseted = blockPos.relative(direction);
        if (offseted.getY() >= world.getMinBuildHeight() && offseted.getY() < world.getMaxBuildHeight()) {
            PacketRedirection.sendRedirectedMessage(
                player,
                dimension,
                new ClientboundBlockUpdatePacket(world, offseted)
            );
        }
    }
    
}
