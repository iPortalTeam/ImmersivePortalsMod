package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PortalWandItem extends Item {
    public static final PortalWandItem instance = new PortalWandItem(new Properties());
    
    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(
            groupEntries -> {
                groupEntries.accept(new ItemStack(instance));
            }
        );
        
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.getMainHandItem().getItem() == instance) {
                // cannot break block using the wand
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
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
        });
        
        IPGlobal.clientCleanupSignal.connect(ClientPortalWandPortalCreation::reset);
    }
    
    public static enum Mode {
        CREATE_PORTAL,
        DRAG_PORTAL;
        
        public static Mode fromTag(CompoundTag tag) {
            String mode = tag.getString("mode");
            
            return switch (mode) {
                case "create_portal" -> CREATE_PORTAL;
                case "drag_portal" -> DRAG_PORTAL;
                default -> CREATE_PORTAL;
            };
        }
        
        public Mode next() {
            return switch (this) {
                case CREATE_PORTAL -> DRAG_PORTAL;
                case DRAG_PORTAL -> CREATE_PORTAL;
            };
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            String modeString = switch (this) {
                case CREATE_PORTAL -> "create_portal";
                case DRAG_PORTAL -> "drag_portal";
            };
            tag.putString("mode", modeString);
            return tag;
        }
        
        public MutableComponent getText() {
            return switch (this) {
                case CREATE_PORTAL -> Component.translatable("imm_ptl.wand.mode.create_portal");
                case DRAG_PORTAL -> Component.translatable("imm_ptl.wand.mode.drag_portal");
            };
        }
        
    }
    
    public PortalWandItem(Properties properties) {
        super(properties);
    }
    
    @Environment(EnvType.CLIENT)
    public static void onClientLeftClick(LocalPlayer player, ItemStack itemStack) {
        if (player.getPose() == Pose.CROUCHING) {
            showSettings(player);
        }
        else {
            Mode mode = Mode.fromTag(itemStack.getOrCreateTag());
            
            switch (mode) {
                case CREATE_PORTAL -> {
                    ClientPortalWandPortalCreation.undo();
                }
                case DRAG_PORTAL -> {
                
                }
            }
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        Mode mode = Mode.fromTag(itemStack.getOrCreateTag());
        
        if (player.getPose() == Pose.CROUCHING) {
            if (!world.isClientSide()) {
                
                Mode nextMode = mode.next();
                itemStack.setTag(nextMode.toTag());
                return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemStack);
            }
        }
        else {
            if (world.isClientSide()) {
                ClientPortalWandPortalCreation.onRightClick();
            }
        }
        
        return super.use(world, player, hand);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);
        
        tooltip.add(Component.translatable("imm_ptl.wand.item_desc_1"));
        tooltip.add(Component.translatable("imm_ptl.wand.item_desc_2"));
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
    }
    
    private static boolean instructionInformed = false;
    
    @Environment(EnvType.CLIENT)
    private static void updateDisplay(ItemStack itemStack) {
        Mode mode = Mode.fromTag(itemStack.getOrCreateTag());
        
        switch (mode) {
            case CREATE_PORTAL -> ClientPortalWandPortalCreation.updateDisplay();
            case DRAG_PORTAL -> ClientPortalWandPortalDrag.updateDisplay();
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static void clearCursorPointing() {
        ClientPortalWandPortalCreation.clearCursorPointing();
    }
    
    @Environment(EnvType.CLIENT)
    public static void clientRender(
        LocalPlayer player, ItemStack itemStack, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        if (!instructionInformed) {
            instructionInformed = true;
            player.sendSystemMessage(
                IPMcHelper.getTextWithCommand(
                    Component.translatable("imm_ptl.show_portal_wand_instruction"),
                    "/imm_ptl_client_debug wand show_instruction"
                )
            );
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
        }
    }
    
}
