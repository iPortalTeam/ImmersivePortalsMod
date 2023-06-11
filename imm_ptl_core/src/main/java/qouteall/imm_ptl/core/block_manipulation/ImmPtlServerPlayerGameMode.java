package qouteall.imm_ptl.core.block_manipulation;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.imm_ptl.core.platform_specific.O_O;

import java.util.Objects;

/**
 * The copy of {@link net.minecraft.server.level.ServerPlayerGameMode} that handles not only one dimension.
 * Use copying instead of Mixin to avoid complexity (using Mixins require tons of injections and is error-prone)
 *  and mod compatibility (switching level field make it not thread-safe with dimensional threading).
 * Changes:
 * - remove level field and pass level by argument.
 * - change player.level() to the argument level
 * - record the level for destroyPos and delayedDestroyPos.
 * - use withForceRedirect
 * - removed the distance check and height check
 * Note: Needs to fire Fabric and Forge events.
 */
@IPVanillaCopy
public class ImmPtlServerPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final ServerPlayer player;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPos destroyPos = BlockPos.ZERO;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPos delayedDestroyPos = BlockPos.ZERO;
    @Nullable
    private ServerLevel destroyPosLevel = null;
    private int delayedTickStart;
    private int lastSentState = -1;
    
    public ImmPtlServerPlayerGameMode(ServerPlayer serverPlayer) {
        this.player = serverPlayer;
    }
    
    public void tick() {
        ++this.gameTicks;
        if (this.hasDelayedDestroy) {
            Validate.notNull(destroyPosLevel);
            PacketRedirection.withForceRedirect(
                destroyPosLevel,
                () -> {
                    BlockState blockState = destroyPosLevel.getBlockState(this.delayedDestroyPos);
                    if (blockState.isAir()) {
                        this.hasDelayedDestroy = false;
                    }
                    else {
                        float f = this.incrementDestroyProgress(destroyPosLevel, blockState, this.delayedDestroyPos, this.delayedTickStart);
                        if (f >= 1.0f) {
                            this.hasDelayedDestroy = false;
                            this.destroyBlock(destroyPosLevel, this.delayedDestroyPos);
                        }
                    }
                }
            );
        }
        else if (this.isDestroyingBlock) {
            Validate.notNull(destroyPosLevel);
            PacketRedirection.withForceRedirect(
                destroyPosLevel,
                () -> {
                    BlockState blockState = destroyPosLevel.getBlockState(this.destroyPos);
                    if (blockState.isAir()) {
                        destroyPosLevel.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                        this.lastSentState = -1;
                        this.isDestroyingBlock = false;
                    }
                    else {
                        this.incrementDestroyProgress(destroyPosLevel, blockState, this.destroyPos, this.destroyProgressStart);
                    }
                }
            );
        }
    }
    
    private float incrementDestroyProgress(ServerLevel level, BlockState state, BlockPos pos, int i) {
        int j = this.gameTicks - i;
        float f = state.getDestroyProgress(this.player, level, pos) * (float) (j + 1);
        int k = (int) (f * 10.0f);
        if (k != this.lastSentState) {
            level.destroyBlockProgress(this.player.getId(), pos, k);
            this.lastSentState = k;
        }
        return f;
    }
    
    private void debugLogging(BlockPos blockPos, boolean bl, int i, String string) {
    }
    
    public void handleBlockBreakAction(
        ServerLevel level,
        BlockPos pos, ServerboundPlayerActionPacket.Action action,
        Direction face, int maxBuildHeight, int sequence
    ) {
        PacketRedirection.withForceRedirect(
            level,
            () -> {
                if (O_O.onHandleBlockBreakAction(player, level, pos, action, face, maxBuildHeight, sequence)) {
                    return;
                }
                
                if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                    if (!level.mayInteract(this.player, pos)) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
                        this.debugLogging(pos, false, sequence, "may not interact");
                        return;
                    }
                    if (player.isCreative()) {
                        this.destroyAndAck(level, pos, sequence, "creative destroy");
                        return;
                    }
                    if (this.player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
                        this.debugLogging(pos, false, sequence, "block action restricted");
                        return;
                    }
                    this.destroyProgressStart = this.gameTicks;
                    float f = 1.0f;
                    BlockState blockState = level.getBlockState(pos);
                    if (!blockState.isAir()) {
                        blockState.attack(level, pos, this.player);
                        f = blockState.getDestroyProgress(this.player, level, pos);
                    }
                    if (!blockState.isAir() && f >= 1.0f) {
                        this.destroyAndAck(level, pos, sequence, "insta mine");
                    }
                    else {
                        if (this.isDestroyingBlock) {
                            this.player.connection.send(new ClientboundBlockUpdatePacket(this.destroyPos, level.getBlockState(this.destroyPos)));
                            this.debugLogging(pos, false, sequence, "abort destroying since another started (client insta mine, server disagreed)");
                        }
                        this.isDestroyingBlock = true;
                        this.destroyPos = pos.immutable();
                        this.destroyPosLevel = level; // NEW
                        int i = (int) (f * 10.0f);
                        level.destroyBlockProgress(this.player.getId(), pos, i);
                        this.debugLogging(pos, true, sequence, "actual start of destroying");
                        this.lastSentState = i;
                    }
                }
                else if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    if (pos.equals(this.destroyPos)) {
                        int j = this.gameTicks - this.destroyProgressStart;
                        BlockState blockState = level.getBlockState(pos);
                        if (!blockState.isAir()) {
                            float g = blockState.getDestroyProgress(this.player, level, pos) * (float) (j + 1);
                            if (g >= 0.7f) {
                                this.isDestroyingBlock = false;
                                level.destroyBlockProgress(this.player.getId(), pos, -1);
                                this.destroyAndAck(level, pos, sequence, "destroyed");
                                return;
                            }
                            if (!this.hasDelayedDestroy) {
                                this.isDestroyingBlock = false;
                                this.hasDelayedDestroy = true;
                                this.delayedDestroyPos = pos;
                                this.destroyPosLevel = level; // NEW
                                this.delayedTickStart = this.destroyProgressStart;
                            }
                        }
                    }
                    this.debugLogging(pos, true, sequence, "stopped destroying");
                }
                else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
                    this.isDestroyingBlock = false;
                    if (!Objects.equals(this.destroyPos, pos)) {
                        LOGGER.warn("Mismatch in destroy block pos: {} {}", (Object) this.destroyPos, (Object) pos);
                        level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                        this.debugLogging(pos, true, sequence, "aborted mismatched destroying");
                    }
                    level.destroyBlockProgress(this.player.getId(), pos, -1);
                    this.debugLogging(pos, true, sequence, "aborted destroying");
                }
            }
        );
    }
    
    public void destroyAndAck(
        ServerLevel level,
        BlockPos pos, int i, String string
    ) {
        PacketRedirection.withForceRedirect(
            level,
            () -> {
                if (this.destroyBlock(level, pos)) {
                    this.debugLogging(pos, true, i, string);
                }
                else {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
                    this.debugLogging(pos, false, i, string);
                }
            }
        );
    }
    
    /**
     * Attempts to harvest a block
     */
    public boolean destroyBlock(ServerLevel level, BlockPos pos) {
        return PacketRedirection.<Boolean>withForceRedirectAndGet(
            level,
            () -> {
                BlockState blockState = level.getBlockState(pos);
                if (!this.player.getMainHandItem().getItem().canAttackBlock(blockState, level, pos, this.player)) {
                    return false;
                }
                BlockEntity blockEntity = level.getBlockEntity(pos);
                Block block = blockState.getBlock();
                if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
                    level.sendBlockUpdated(pos, blockState, blockState, 3);
                    return false;
                }
                if (this.player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
                    return false;
                }
                block.playerWillDestroy(level, pos, blockState, this.player);
                boolean bl = level.removeBlock(pos, false);
                if (bl) {
                    block.destroy(level, pos, blockState);
                }
                if (player.isCreative()) {
                    return true;
                }
                ItemStack itemStack = this.player.getMainHandItem();
                ItemStack itemStack2 = itemStack.copy();
                boolean bl2 = this.player.hasCorrectToolForDrops(blockState);
                itemStack.mineBlock(level, blockState, pos, this.player);
                if (bl && bl2) {
                    block.playerDestroy(level, this.player, pos, blockState, blockEntity, itemStack2);
                }
                return true;
            }
        );
    }
    
    public InteractionResult useItem(ServerPlayer player, ServerLevel level, ItemStack stack, InteractionHand hand) {
        return PacketRedirection.<InteractionResult>withForceRedirectAndGet(
            level,
            () -> {
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    return InteractionResult.PASS;
                }
                if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                    return InteractionResult.PASS;
                }
                int i = stack.getCount();
                int j = stack.getDamageValue();
                InteractionResultHolder<ItemStack> interactionResultHolder = stack.use(level, player, hand);
                ItemStack itemStack = interactionResultHolder.getObject();
                if (itemStack == stack && itemStack.getCount() == i && itemStack.getUseDuration() <= 0 && itemStack.getDamageValue() == j) {
                    return interactionResultHolder.getResult();
                }
                if (interactionResultHolder.getResult() == InteractionResult.FAIL && itemStack.getUseDuration() > 0 && !player.isUsingItem()) {
                    return interactionResultHolder.getResult();
                }
                if (stack != itemStack) {
                    player.setItemInHand(hand, itemStack);
                }
                if (player.isCreative() && itemStack != ItemStack.EMPTY) {
                    itemStack.setCount(i);
                    if (itemStack.isDamageableItem() && itemStack.getDamageValue() != j) {
                        itemStack.setDamageValue(j);
                    }
                }
                if (itemStack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                if (!player.isUsingItem()) {
                    player.inventoryMenu.sendAllDataToRemote();
                }
                return interactionResultHolder.getResult();
            }
        );
    }
    
    public InteractionResult useItemOn(
        ServerPlayer player, ServerLevel level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult
    ) {
        return PacketRedirection.<InteractionResult>withForceRedirectAndGet(
            level,
            () -> {
                InteractionResult interactionResult2;
                InteractionResult interactionResult;
                BlockPos blockPos = hitResult.getBlockPos();
                BlockState blockState = level.getBlockState(blockPos);
                if (!blockState.getBlock().isEnabled(level.enabledFeatures())) {
                    return InteractionResult.FAIL;
                }
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    MenuProvider menuProvider = blockState.getMenuProvider(level, blockPos);
                    if (menuProvider != null) {
                        player.openMenu(menuProvider);
                        return InteractionResult.SUCCESS;
                    }
                    return InteractionResult.PASS;
                }
                boolean bl = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
                boolean bl2 = player.isSecondaryUseActive() && bl;
                ItemStack itemStack = stack.copy();
                if (!bl2 && (interactionResult = blockState.use(level, player, hand, hitResult)).consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockPos, itemStack);
                    return interactionResult;
                }
                if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack.getItem())) {
                    return InteractionResult.PASS;
                }
                UseOnContext useOnContext = new UseOnContext(player, hand, hitResult);
                if (player.isCreative()) {
                    int i = stack.getCount();
                    interactionResult2 = stack.useOn(useOnContext);
                    stack.setCount(i);
                }
                else {
                    interactionResult2 = stack.useOn(useOnContext);
                }
                if (interactionResult2.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, blockPos, itemStack);
                }
                return interactionResult2;
            }
        );
        
    }
}
