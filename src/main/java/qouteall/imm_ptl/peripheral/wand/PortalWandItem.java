package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;

import java.util.ArrayList;
import java.util.List;

public class PortalWandItem extends Item {
    public static final PortalWandItem instance = new PortalWandItem(new Properties());
    
    public static void init() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getMainHandItem().getItem() == instance) {
                // cannot break block using the wand
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        
        BlockManipulationServer.canDoCrossPortalInteractionEvent.register(p -> {
            return p.getMainHandItem().getItem() != instance;
        });
    }
    
    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                ItemStack itemStack = client.player.getMainHandItem();
                if (itemStack.getItem() == instance) {
                    updateDisplay(itemStack);
                }
                else {
                    ClientPortalWandPortalCreation.clearCursorPointing();
                }
            }
            ClientPortalWandPortalDrag.tick();
        });
        
        
        IPCGlobal.CLIENT_CLEANUP_EVENT.register(ClientPortalWandPortalCreation::reset);
        IPCGlobal.CLIENT_CLEANUP_EVENT.register(ClientPortalWandPortalDrag::reset);
        IPCGlobal.CLIENT_CLEANUP_EVENT.register(ClientPortalWandPortalCopy::reset);
    }
    
    public static void addIntoCreativeTag(CreativeModeTab.Output entries) {
        ItemStack w1 = new ItemStack(instance);
        w1.setTag(Mode.CREATE_PORTAL.toTag());
        entries.accept(w1);
        
        ItemStack w2 = new ItemStack(instance);
        w2.setTag(Mode.DRAG_PORTAL.toTag());
        entries.accept(w2);
        
        ItemStack w3 = new ItemStack(instance);
        w3.setTag(Mode.COPY_PORTAL.toTag());
        entries.accept(w3);
    }
    
    public static enum Mode {
        CREATE_PORTAL,
        DRAG_PORTAL,
        COPY_PORTAL;
        
        public static Mode fromTag(CompoundTag tag) {
            String mode = tag.getString("mode");
            
            return switch (mode) {
                case "create_portal" -> CREATE_PORTAL;
                case "drag_portal" -> DRAG_PORTAL;
                case "copy_portal" -> COPY_PORTAL;
                default -> CREATE_PORTAL;
            };
        }
        
        public Mode next() {
            return switch (this) {
                case CREATE_PORTAL -> DRAG_PORTAL;
                case DRAG_PORTAL -> COPY_PORTAL;
                case COPY_PORTAL -> CREATE_PORTAL;
            };
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            String modeString = switch (this) {
                case CREATE_PORTAL -> "create_portal";
                case DRAG_PORTAL -> "drag_portal";
                case COPY_PORTAL -> "copy_portal";
            };
            tag.putString("mode", modeString);
            return tag;
        }
        
        public MutableComponent getText() {
            return switch (this) {
                case CREATE_PORTAL -> Component.translatable("imm_ptl.wand.mode.create_portal");
                case DRAG_PORTAL -> Component.translatable("imm_ptl.wand.mode.drag_portal");
                case COPY_PORTAL -> Component.translatable("imm_ptl.wand.mode.copy_portal");
            };
        }
        
    }
    
    public PortalWandItem(Properties properties) {
        super(properties);
    }
    
    @Environment(EnvType.CLIENT)
    public static void onClientLeftClick(LocalPlayer player, ItemStack itemStack) {
        if (player.isShiftKeyDown()) {
            showSettings(player);
        }
        else {
            Mode mode = Mode.fromTag(itemStack.getOrCreateTag());
            
            switch (mode) {
                case CREATE_PORTAL -> {
                    ClientPortalWandPortalCreation.onLeftClick();
                }
                case DRAG_PORTAL -> {
                    ClientPortalWandPortalDrag.onLeftClick();
                }
                case COPY_PORTAL -> {
                    ClientPortalWandPortalCopy.onLeftClick();
                }
            }
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        Mode mode = Mode.fromTag(itemStack.getOrCreateTag());
        
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                if (!PortalWandInteraction.isDragging(((ServerPlayer) player))) {
                    Mode nextMode = mode.next();
                    itemStack.setTag(nextMode.toTag());
                    return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemStack);
                }
            }
        }
        
        if (!player.isShiftKeyDown()) {
            if (world.isClientSide()) {
                onUseClient(mode);
            }
        }
        
        return super.use(world, player, hand);
    }
    
    @Environment(EnvType.CLIENT)
    private void onUseClient(Mode mode) {
        switch (mode) {
            case CREATE_PORTAL -> {
                ClientPortalWandPortalCreation.onRightClick();
            }
            case DRAG_PORTAL -> {
                ClientPortalWandPortalDrag.onRightClick();
            }
            case COPY_PORTAL -> {
                ClientPortalWandPortalCopy.onRightClick();
            }
        }
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        
        tooltip.add(Component.translatable(
            "imm_ptl.wand.item_desc_1",
            Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage(),
            Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage()
        ));
        tooltip.add(Component.translatable(
            "imm_ptl.wand.item_desc_2",
            Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage(),
            Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage()
        ));
    }
    
    @Override
    public Component getName(ItemStack stack) {
        Mode mode = Mode.fromTag(stack.getOrCreateTag());
        
        MutableComponent baseText = Component.translatable("item.immersive_portals.portal_wand");
        
        return baseText
            .append(Component.literal(" : "))
            .append(mode.getText().withStyle(ChatFormatting.GOLD));
    }
    
    public static void showSettings(Player player) {
        player.sendSystemMessage(Component.translatable("imm_ptl.wand.settings_1"));
        player.sendSystemMessage(Component.translatable("imm_ptl.wand.settings_alignment"));
        
        int[] alignments = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 32, 64};
        
        List<MutableComponent> alignmentSettingTexts = new ArrayList<>();
        for (int alignment : alignments) {
            MutableComponent textWithCommand = IPMcHelper.getTextWithCommand(
                Component.literal("1/" + alignment),
                "/imm_ptl_client_debug wand set_cursor_alignment " + alignment
            );
            alignmentSettingTexts.add(textWithCommand);
        }
        
        alignmentSettingTexts.add(IPMcHelper.getTextWithCommand(
            Component.translatable("imm_ptl.wand.no_alignment"),
            "/imm_ptl_client_debug wand set_cursor_alignment 0"
        ));
        
        player.sendSystemMessage(
            alignmentSettingTexts.stream().reduce(Component.literal(""), (a, b) -> a.append(" ").append(b))
        );
        
        player.sendSystemMessage(Component.translatable(
            "imm_ptl.wand.settings_2", Minecraft.getInstance().options.keyChat.getTranslatedKeyMessage()
        ));
    }
    
    private static boolean instructionInformed = false;
    
    @Environment(EnvType.CLIENT)
    private static void updateDisplay(ItemStack itemStack) {
        Mode mode = Mode.fromTag(itemStack.getOrCreateTag());
        
        switch (mode) {
            case CREATE_PORTAL -> ClientPortalWandPortalCreation.updateDisplay();
            case DRAG_PORTAL -> ClientPortalWandPortalDrag.updateDisplay();
            case COPY_PORTAL -> ClientPortalWandPortalCopy.updateDisplay();
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void clientRender(
        LocalPlayer player, ItemStack itemStack, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        if (!instructionInformed) {
            instructionInformed = true;
//            player.sendSystemMessage(
//                IPMcHelper.getTextWithCommand(
//                    Component.translatable("imm_ptl.show_portal_wand_instruction"),
//                    "/imm_ptl_client_debug wand show_instruction"
//                )
//            );
        }
        
        CompoundTag tag = itemStack.getOrCreateTag();
        Mode mode = Mode.fromTag(tag);
        
        switch (mode) {
            case CREATE_PORTAL -> ClientPortalWandPortalCreation.render(
                poseStack, bufferSource, camX, camY, camZ
            );
            case DRAG_PORTAL -> ClientPortalWandPortalDrag.render(
                poseStack, bufferSource, camX, camY, camZ
            );
            case COPY_PORTAL -> ClientPortalWandPortalCopy.render(
                poseStack, bufferSource, camX, camY, camZ
            );
        }
    }
    
}
