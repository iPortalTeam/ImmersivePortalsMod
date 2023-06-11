package qouteall.imm_ptl.core.block_manipulation;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
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
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class BlockManipulationServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
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
    
    @Deprecated
    public static void processBreakBlock(
        ResourceKey<Level> dimension,
        ServerboundPlayerActionPacket packet,
        ServerPlayer player
    ) {
        if (shouldFinishMining(dimension, packet, player)) {
            if (canPlayerReach(dimension, player, packet.getPos())) {
                doDestroyBlock(dimension, packet, player);
            }
            else {
                Helper.log("Rejected cross portal block breaking packet " + player);
            }
        }
    }
    
    @Deprecated
    private static boolean shouldFinishMining(
        ResourceKey<Level> dimension,
        ServerboundPlayerActionPacket packet,
        ServerPlayer player
    
    ) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            return canInstantMine(
                MiscHelper.getServer().getLevel(dimension),
                player,
                packet.getPos()
            );
        }
        else {
            return packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
        }
    }
    
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
    
    @Deprecated
    @IPVanillaCopy
    private static void doDestroyBlock(
        ResourceKey<Level> dimension,
        ServerboundPlayerActionPacket packet,
        ServerPlayer player
    ) {
        ServerLevel destWorld = MiscHelper.getServer().getLevel(dimension);
        ServerLevel oldWorld = player.serverLevel();
        
        BlockPos blockPos = packet.getPos();
        
        if (destWorld.mayInteract(player, blockPos)) {
            player.gameMode.setLevel(destWorld);
            player.gameMode.destroyBlock(
                blockPos
            );
            player.gameMode.setLevel(oldWorld);
        }
        else {
            ClientboundBlockUpdatePacket ackPacket = new ClientboundBlockUpdatePacket(
                blockPos, destWorld.getBlockState(blockPos)
            );
            player.connection.send(PacketRedirection.createRedirectedMessage(dimension, ackPacket));
        }
    }
    
    @Deprecated
    @IPVanillaCopy
    private static boolean canInstantMine(
        ServerLevel world,
        ServerPlayer player,
        BlockPos pos
    ) {
        if (player.isCreative()) {
            return true;
        }
        
        float progress = 1.0F;
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir()) {
            blockState.attack(world, pos, player);
            progress = blockState.getDestroyProgress(player, world, pos);
        }
        return !blockState.isAir() && progress >= 1.0F;
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
    
    @Deprecated
    public static void processRightClickBlock(
        ResourceKey<Level> dimension,
        ServerboundUseItemOnPacket packet,
        ServerPlayer player,
        int sequenceNumber
    ) {
        InteractionHand hand = packet.getHand();
        BlockHitResult blockHitResult = packet.getHitResult();
        
        ServerLevel world = MiscHelper.getServer().getLevel(dimension);
        
        doProcessRightClick(dimension, player, hand, blockHitResult, sequenceNumber);
    }
    
    /**
     * {@link net.minecraft.server.network.ServerGamePacketListenerImpl#handleUseItemOn(ServerboundUseItemOnPacket)}
     */
    @Deprecated
    @IPVanillaCopy
    public static void doProcessRightClick(
        ResourceKey<Level> dimension,
        ServerPlayer player,
        InteractionHand hand,
        BlockHitResult blockHitResult,
        int sequenceNumber
    ) {
        player.connection.ackBlockChangesUpTo(sequenceNumber);
        
        ItemStack itemStack = player.getItemInHand(hand);
        
        MinecraftServer server = MiscHelper.getServer();
        ServerLevel targetWorld = server.getLevel(dimension);
        Validate.notNull(targetWorld);
        
        if (!itemStack.isItemEnabled(targetWorld.enabledFeatures())) {
            return;
        }
        
        BlockPos blockPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getDirection();
        player.resetLastActionTime();
        if (targetWorld.mayInteract(player, blockPos)) {
            if (!canPlayerReach(dimension, player, blockPos)) {
                Helper.log("Reject cross portal block placing packet " + player);
                return;
            }
            
            Level oldWorld = player.level();
            
            ((IEEntity) player).ip_setWorld(targetWorld);
            try {
                InteractionResult actionResult = player.gameMode.useItemOn(
                    player,
                    targetWorld,
                    itemStack,
                    hand,
                    blockHitResult
                );
                if (actionResult.shouldSwing()) {
                    player.swing(hand, true);
                }
            }
            finally {
                ((IEEntity) player).ip_setWorld(oldWorld);
            }
        }
        
        PacketRedirection.sendRedirectedMessage(
            player,
            dimension,
            new ClientboundBlockUpdatePacket(targetWorld, blockPos)
        );
        
        BlockPos offseted = blockPos.relative(direction);
        if (offseted.getY() >= targetWorld.getMinBuildHeight() && offseted.getY() < targetWorld.getMaxBuildHeight()) {
            PacketRedirection.sendRedirectedMessage(
                player,
                dimension,
                new ClientboundBlockUpdatePacket(targetWorld, offseted)
            );
        }
    }
    
    public static class RemoteCallables {
        public static void processPlayerActionPacket(
            ServerPlayer player,
            ResourceKey<Level> dimension,
            byte[] packetBytes
        ) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(packetBytes));
            ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(buf);
            
            ServerLevel world = MiscHelper.getServer().getLevel(dimension);
            
            doProcessPlayerAction(world, player, packet);
        }
    }
    
    // the key is player UUID
    private static final Map<UUID, ImmPtlServerPlayerGameMode> GAME_MODE_MAP =
        new WeakHashMap<>();
    
    public static void init() {
        IPGlobal.serverCleanupSignal.connect(GAME_MODE_MAP::clear);
        
        IPGlobal.postServerTickSignal.connect(() -> {
            GAME_MODE_MAP.entrySet().removeIf(e -> {
                ImmPtlServerPlayerGameMode mode = e.getValue();
                if (mode == null) {
                    return true;
                }
                if (mode.player.isRemoved()) {
                    return true;
                }
                
                // tick it here
                mode.tick();
                
                return false;
            });
        });
    }
    
    /**
     * {@link net.minecraft.server.network.ServerGamePacketListenerImpl#handlePlayerAction(ServerboundPlayerActionPacket)}
     */
    @IPVanillaCopy
    private static void doProcessPlayerAction(ServerLevel world, ServerPlayer player, ServerboundPlayerActionPacket packet) {
        player.resetLastActionTime();
        BlockPos blockPos = packet.getPos();
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK ||
            action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
            action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
        ) {
            ImmPtlServerPlayerGameMode gameMode = GAME_MODE_MAP.computeIfAbsent(player.getUUID(), k -> new ImmPtlServerPlayerGameMode(player));
            
            gameMode.handleBlockBreakAction(
                world,
                blockPos, action, packet.getDirection(),
                player.level().getMaxBuildHeight(), packet.getSequence()
            );
            player.connection.ackBlockChangesUpTo(packet.getSequence());
        }
    }
    
}
